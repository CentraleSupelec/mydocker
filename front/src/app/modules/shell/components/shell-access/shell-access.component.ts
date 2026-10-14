import { ChangeDetectorRef, Component, Inject, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { APP_CONFIG, DEFAULT_START_RESPONSE_TIMEOUT_IN_SECONDS, IAppConfig } from '../../../../app-config';
import { EMPTY, interval, of, Subject, Subscription } from "rxjs";
import { ContainerApiService } from "../../services/container-api.service";
import { catchError, map, mergeMap, switchMap, takeUntil } from "rxjs/operators";
import {
  ContainerStatus,
  ContainerSwarmState,
  IContainer,
} from "../../interfaces/container";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { ConfirmDialogService } from "../../../utils/confirm-dialog/confirm-dialog.service";
import { ISession } from "../../interfaces/session";
import { IBasicCourse } from "../../interfaces/course";
import { OpenLogDialogService } from "../../../log-dialog/open-log-dialog.service";
import { SnackNotificationService } from '../../../utils/snack-notification/snack-notification.service';
import { DesktopNotificationService } from '../../../utils/services/desktop-notification.service';
import { NgxPermissionsObject, NgxPermissionsService } from "ngx-permissions";
import { Roles } from "../../../admin-users/interfaces/roles";
import { TranslateService } from '@ngx-translate/core';
import { LaunchIntent } from '../../interfaces/launch-intent';
import { nextRetryDecision } from './next-retry-decision';


const ContainerSwarmStateOrder = [
  ContainerSwarmState.NEW,
  ContainerSwarmState.PENDING,
  ContainerSwarmState.ASSIGNED,
  ContainerSwarmState.ACCEPTED,
  ContainerSwarmState.PREPARING,
  ContainerSwarmState.READY,
  ContainerSwarmState.STARTING,
  ContainerSwarmState.RUNNING,
];

interface IStartError {
  status?: number;
  message?: string;
  error?: unknown;
}

interface IPolling {
  container: IContainer | null;
  recovered: boolean;
  error?: IStartError;
  /** The failure is that of a recovery, which is a start request of its own. */
  startFailed?: boolean;
}

@Component({
  selector: 'app-shell-access',
  templateUrl: './shell-access.component.html',
  styleUrls: ['./shell-access.component.scss']
})
export class ShellAccessComponent implements OnInit, OnDestroy, OnChanges {
  @Input() $startInitPolling: Subject<void> = new Subject<void>();
  @Input() $reset: Subject<void> = new Subject<void>();
  @Input() session: ISession | null = null;
  @Input() course: IBasicCourse | undefined = undefined;
  @Input() intent: LaunchIntent = 'none';
  @Input() active: boolean = false;
  @Input() userRedirect: string | undefined = undefined;

  container: IContainer | null = null;
  /** True only after the student asked for this environment in this page session. */
  userStarted = false;
  /** True when the last start request failed or went unanswered, which is what offers a retry. */
  startFailed = false;
  /** The technical reason of the last failed start, when the back end gave one. */
  startError: string | null = null;
  state: 'ask' | 'loading_init' | 'loading_shutdown' | 'container_created' | 'pending' | 'unavailable' | 'start_failed' = 'ask';
  step: number = 0;
  stepMessage: string = '';
  recovering = false;
  private failedAttempts = 0;
  private recoveryPosted = false;
  /** A creation request is only ever re-issued by a panel that was asked to create one. */
  private recoveryAllowed = false;

  private readonly stopInitPolling$: Subject<void> = new Subject<void>();
  private readonly stopShutdownPolling$: Subject<void> = new Subject<void>();
  private readonly destroy$: Subject<void> = new Subject<void>();
  waitingForDeletionDelay = false;

  private askTimeoutId?: number;
  private warningTimeoutId?: number;
  /** One per start request: armed with it, disarmed by the first record of the environment. */
  private startTimeoutId?: number;
  private startRequest?: Subscription;
  /** The one recovery request outstanding, kept apart from the polling ticks so that none cancels it. */
  private recoveryRequest?: Subscription;
  /** What the last start asked for, so that a retry asks for the same. */
  private lastStart = { forceRecreate: false, updateLastStartDate: true };
  private userPermissions?: NgxPermissionsObject;

  constructor(
    private readonly containerApiService: ContainerApiService,
    private readonly toastService: ObservableSnackNotificationService,
    private readonly dialogConfirmService: ConfirmDialogService,
    private readonly openLogDialogService: OpenLogDialogService,
    private readonly cd: ChangeDetectorRef,
    private readonly snackNotificationService: SnackNotificationService,
    private readonly desktopNotificationService: DesktopNotificationService,
    private readonly ngxPermissionsService: NgxPermissionsService,
    private readonly translate: TranslateService,
    @Inject(APP_CONFIG) readonly config: IAppConfig,
  ) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.active && !this.active) {
      // Unconditionally: a start whose environment has stopped is spent, even if the panel is still
      // in the ask state because its read has not come back yet.
      this.userStarted = false;
      if (!(this.state == 'ask')) {
        this.container = null;
        this.state = 'ask';
      }
    }

    if (changes.intent && 'start' === this.intent && this.canAskContainer()) {
      this.userStarted = true;
      if (this.active) {
        // Already running: there is nothing to create, only details to read.
        this.discoverRunningContainer();
      } else {
        this.initGetContainer();
      }
      return;
    }

    if (changes.active && this.active && 'ask' === this.state) {
      this.discoverRunningContainer();
    }
  }

  ngOnInit(): void {
    this.$startInitPolling
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
      () => this.initGetContainer()
      );

    this.$reset
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
      () => {
        this.stopInitPolling$.next();
        this.clearStartTimeout();
        this.container = null;
      }
    );

    this.userPermissions = this.ngxPermissionsService.getPermissions();
    this.desktopNotificationService.askPermissions();
  }

  /**
   * Reads the details of an environment the dashboard already reports as running. A read only: a
   * dashboard load must never create an environment, and must never open a tab on its own, which is
   * why autoclick follows `userStarted` and not this path.
   */
  private discoverRunningContainer(): void {
    this.containerApiService.getContainer(this.sessionCourseOrCourse?.id).subscribe({
      next: (container) => {
        if (!container || !this.active) {
          // Either the list and this read disagree, or the environment stopped while the read was in
          // flight. The next list poll settles it; nothing is retried here.
          return;
        }
        this.container = container;
        if (this.isReady(container)) {
          this.state = 'container_created';
          // Reaching the created state is what schedules the shutdown warning; skipping it would
          // silently drop the warning for every student who reloads the dashboard.
          this.setWarningTimeout();
        } else {
          // Still starting: show the existing progress. Read-only, hence no recovery request: this
          // panel was never asked to create anything.
          this.startInitPolling(false);
        }
      },
      error: () => {
        this.container = null;
      }
    });
  }

  private isReady(container: IContainer): boolean {
    return container.status === ContainerStatus.OK
      || (container.state === ContainerSwarmState.RUNNING && container.status !== ContainerStatus.CHECKING);
  }

  initGetContainer(forceRecreate: boolean = false, updateLastStartDate: boolean = true) {
    // A creation request issued by this panel only ever comes from the student: the button, the
    // recreate dialog, or a start carried in the address. The read-only discovery path never gets
    // here, which is what keeps a dashboard load from connecting.
    this.userStarted = true;
    this.startFailed = false;
    this.startError = null;
    this.lastStart = { forceRecreate, updateLastStartDate };
    // The request itself may take minutes, the volume precreation included, so the loader shows
    // from the moment it is sent, and the bound covers it too.
    this.state = 'loading_init';
    this.armStartTimeout();
    this.startRequest?.unsubscribe();
    this.startRequest = this.containerApiService.initGetContainer(this.session?.id, forceRecreate, updateLastStartDate)
      .subscribe({
        next: () => this.startInitPolling(),
        error: (err: IStartError) => {
          console.error(this.translate.instant('container.initializing_error'), err);
          this.failStart(err);
        }
      });
  }

  private startInitPolling(allowRecovery: boolean = true) {
    this.recoveryAllowed = allowRecovery;
    this.state = 'loading_init';
    interval(3000).pipe(
      takeUntil(this.stopInitPolling$),
      switchMap(() => {
        if (this.recovering) {
          // The recovery request answers on its own subscription; a tick only sends it if none is out.
          this.sendRecoveryRequest();
          return EMPTY;
        } else {
          return this.containerApiService.getContainer(this.sessionCourseOrCourse?.id).pipe(
            map(container => ({ container, recovered: false } as  IPolling)),
            catchError(err => {
              console.error(this.translate.instant('container.fetch_error'), err);
              return of({ container: null, recovered: false, error: err } as  IPolling);
            })
          );
        }
      })
    )
    .subscribe((pollingResult: IPolling) => this.handlePollingResult(pollingResult))
  }

  /**
   * Start requests may take minutes, so the recovery request is never re-sent while it is out: it
   * stays outstanding until it answers or its bound, armed as it is sent, expires.
   */
  private sendRecoveryRequest(): void {
    if (this.recoveryRequest && !this.recoveryRequest.closed) {
      return;
    }
    this.armStartTimeout();
    this.recoveryRequest = this.containerApiService.initGetContainer(this.session?.id, false).pipe(
      takeUntil(this.stopInitPolling$),
      takeUntil(this.destroy$),
      map(() => ({ container: null, recovered: true } as  IPolling)),
      catchError(err => {
        console.error(this.translate.instant('container.initializing_error'), err);
        return of({ container: null, recovered: false, error: err, startFailed: true } as  IPolling);
      })
    )
    .subscribe((pollingResult: IPolling) => this.handlePollingResult(pollingResult));
  }

  private handlePollingResult(pollingResult: IPolling): void {
    let container = pollingResult.container
    if (pollingResult.error) {
      if (pollingResult.startFailed) {
        this.failStart(pollingResult.error);
      } else {
        this.handlePollingFailure(pollingResult.error);
      }
      return;
    }

    if (pollingResult.recovered && this.recovering) {
      this.recovering = false;
      return;
    }

    this.failedAttempts = 0;
    this.recoveryPosted = false;

    if (container) {
      // Any record, even a failed one, answers the start: from here the existing states say how it went.
      this.clearStartTimeout();
      this.container = container;
      const index = ContainerSwarmStateOrder.indexOf(ContainerSwarmState[container.state as keyof typeof ContainerSwarmState]);
      this.step =  index * 100 / ContainerSwarmStateOrder.length;
      this.stepMessage = `${this.translate.instant(`container.steps.${(container.state ?? ContainerSwarmState.UNKNOWN).toLowerCase()}`)} (${this.translate.instant('container.step')} ${index + 1}/${ContainerSwarmStateOrder.length})`;
      if (this.isReady(container)) {
        this.state = 'container_created';
        this.stopInitPolling$.next();
        this.setWarningTimeout();
      } else if ([ContainerSwarmState.FAILED, ContainerSwarmState.REJECTED, ContainerSwarmState.SHUTDOWN].includes(container.state as ContainerSwarmState) || container.status === ContainerStatus.KO) {
        this.state = 'container_created';
        this.stopInitPolling$.next();
      } else {
        this.state = 'pending';
      }
    }
  }

  /**
   * A failed status read used to switch the loop to re-creating the environment every three seconds,
   * with no end. One creation request may recover a lost environment; the rest is reading, and after
   * a bounded number of failures the panel says so and waits for the student.
   */
  private handlePollingFailure(error: IStartError): void {
    this.failedAttempts++;

    if ('retry' === nextRetryDecision(error, this.failedAttempts)) {
      // A recovery is a start request like any other: its bound is armed when it is sent.
      this.recovering = this.recoveryAllowed && !this.recoveryPosted;
      this.recoveryPosted = this.recoveryPosted || this.recovering;
      return;
    }

    this.recovering = false;
    if (this.recoveryAllowed && undefined !== this.startTimeoutId) {
      // No record has answered the start yet: it ends as an unanswered start, retried as a start.
      this.failStart(error);
      return;
    }
    this.stopInitPolling$.next();
    // The panel already waits for the student; a later timeout would only replace one failure with another.
    this.clearStartTimeout();
    this.state = 'unavailable';
    this.snackNotificationService.push(this.translate.instant('container.status_unavailable'), 'error');
  }

  /**
   * Ends a start that failed or that nothing answered within the configured bound: the loader stops
   * and the generic startup error shows, with the technical reason when there is one.
   */
  private failStart(error?: IStartError): void {
    this.clearStartTimeout();
    this.startRequest?.unsubscribe();
    this.stopInitPolling$.next();
    this.recovering = false;
    this.startFailed = true;
    this.startError = this.describeStartError(error);
    this.state = 'start_failed';
  }

  private describeStartError(error?: IStartError): string | null {
    const body = error?.error as { message?: unknown } | string | null | undefined;
    if (typeof body === 'string' && body) {
      return body;
    }
    if (body && typeof body === 'object' && typeof body.message === 'string' && body.message) {
      return body.message;
    }
    return error?.message || null;
  }

  private armStartTimeout(): void {
    this.clearStartTimeout();
    const seconds = this.config.start_response_timeout_in_seconds ?? DEFAULT_START_RESPONSE_TIMEOUT_IN_SECONDS;
    this.startTimeoutId = window.setTimeout(() => this.failStart(), 1000 * seconds);
  }

  private clearStartTimeout(): void {
    window.clearTimeout(this.startTimeoutId);
    this.startTimeoutId = undefined;
  }

  retryAfterFailure(): void {
    if (this.startFailed) {
      // Nothing came of the last start request, so the retry is a new one, asking for the same.
      this.initGetContainer(this.lastStart.forceRecreate, this.lastStart.updateLastStartDate);
      return;
    }
    this.failedAttempts = 0;
    this.recoveryPosted = false;
    this.recovering = false;
    if (this.recoveryAllowed) {
      // A panel that asked for the environment waits for it within the same bound again.
      this.armStartTimeout();
    }
    // Retrying keeps the panel in the mode it was in: a watcher stays a watcher.
    this.startInitPolling(this.recoveryAllowed);
  }

  private async setWarningTimeout() {
    if (
      !this.sessionCourseOrCourse?.shutdownAfterMinutes ||
      !this.sessionCourseOrCourse?.warnShutdownMinutes ||
      !this.container?.deletionTime ||
      this.container.status === 'KO'
    ) {
      return;
    }
    await this.desktopNotificationService.askPermissions();
    const warningTimeSeconds = this.container?.deletionTime - 60
      * this.sessionCourseOrCourse?.warnShutdownMinutes;
    const timeoutDuration = 1000 * warningTimeSeconds - Date.now();
    if (timeoutDuration > 0) {
      this.warningTimeoutId = window.setTimeout(() => {
        this.desktopNotificationService.notify(
          this.translate.instant('container.shutdown_notification'),
          this.translate.instant('container.shutdown_delay_explanation')
        );
        window.clearTimeout(this.warningTimeoutId);
        }, timeoutDuration
      );
    }
  }


  private startShutdownPolling() {
    this.state = 'loading_shutdown';
    interval(1000).pipe(
      takeUntil(this.stopShutdownPolling$),
      mergeMap(() => this.containerApiService.getShutdownStatus(this.sessionCourseOrCourse?.id))
    ).subscribe(
      container => {
        if (container?.isShutdown) {
          this.container = null;
          this.state = 'ask';
          window.clearTimeout(this.warningTimeoutId);
          this.stopShutdownPolling$.next();
        } else if (container?.error) {
          this.state = 'container_created';
          this.snackNotificationService.push(this.translate.instant('container.cannot_shutdown'), 'error');
          this.stopShutdownPolling$.next();
        }
      }
    )
  }

  askNewEnvWithConfirmationDialog() {
    this.dialogConfirmService.confirm({
      title: this.translate.instant('container.confirm_env_request'),
      text: this.translate.instant('container.confirm_env_request_warning')
    }).subscribe(
      (confirm:boolean) => {
        if(confirm) {
          this.container = null;
          this.initGetContainer(true);
        }
      }
    )
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.clearStartTimeout();
    if (this.askTimeoutId) {
      window.clearTimeout(this.askTimeoutId);
      window.clearTimeout(this.warningTimeoutId);
    }
  }

  get sessionCourseOrCourse(): IBasicCourse | undefined {
    return this.session?.course || this.course
  }

  canAskContainer(): boolean {
    if (this.session) {
      const canAskContainer = !this.session.blockContainerCreationBeforeStartTime
        || this.session.startDateTime < new Date().getTime()
        || this.canEditCourse(this.course?.id);
      if (!canAskContainer && !this.askTimeoutId) {
        this.askTimeoutId = window.setTimeout(
          () => this.cd.detectChanges(),
          this.session.startDateTime - new Date().getTime()
        );
      }
      return canAskContainer;
    }
    return false;
  }

  canEditCourse(courseId?: number): boolean {
    return !!this.userPermissions?.[`course.${courseId}.edit`]
      || !!this.userPermissions?.[`course.${courseId}.creator`]
      || !!this.userPermissions?.[Roles.admin];
  }

  fetchLogs() {
    this.containerApiService.getLogs(this.sessionCourseOrCourse?.id).subscribe(
      logs => this.openLogDialogService.openDialog(logs)
    )
  }

  deleteEnvWithConfirmationDialog() {
    this.dialogConfirmService
      .confirm({
        text: this.translate.instant('container.confirm_shutdown'),
      })
      .subscribe((confirm:boolean) => {
        if(confirm) {
          this.deleteEnv();
        }
      });
  }

  deleteEnv() {
    this.stopInitPolling$.next();
    this.containerApiService.shutdownContainer(this.sessionCourseOrCourse?.id).subscribe(() => {
      this.startShutdownPolling();
    });
  }

  onTimerElapsed() {
    this.container = null;
    this.state = 'ask';
  }
}
