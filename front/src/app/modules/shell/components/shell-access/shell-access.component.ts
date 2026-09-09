import { ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { interval, of, Subject } from "rxjs";
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

interface IPolling {
  container: IContainer | null;
  recovered: boolean;
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
  state: 'ask' | 'loading_init' | 'loading_shutdown' | 'container_created' | 'pending' = 'ask';
  step: number = 0;
  stepMessage: string = '';
  recovering = false;

  private readonly stopInitPolling$: Subject<void> = new Subject<void>();
  private readonly stopShutdownPolling$: Subject<void> = new Subject<void>();
  private readonly destroy$: Subject<void> = new Subject<void>();
  waitingForDeletionDelay = false;

  private askTimeoutId?: number;
  private warningTimeoutId?: number;
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
    private readonly translate: TranslateService
  ) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!(this.state == 'ask') && changes.active && !this.active) {
      this.container = null;
      this.state = 'ask';
      this.userStarted = false;
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
        if (!container) {
          // The list and this read disagree; the next list poll settles it, no retry here.
          return;
        }
        this.container = container;
        if (this.isReady(container)) {
          this.state = 'container_created';
          // Reaching the created state is what schedules the shutdown warning; skipping it would
          // silently drop the warning for every student who reloads the dashboard.
          this.setWarningTimeout();
        } else {
          // Still starting: show the existing progress, which reads and never creates.
          this.startInitPolling();
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
    this.containerApiService.initGetContainer(this.session?.id, forceRecreate, updateLastStartDate)
      .subscribe(
        () => this.startInitPolling()
      );
  }

  private startInitPolling() {
    this.state = 'loading_init';
    interval(3000).pipe(
      takeUntil(this.stopInitPolling$),
      switchMap(() => {
        if (this.recovering) {
          return this.containerApiService.initGetContainer(this.session?.id, false).pipe(
            map(() => ({ container: null, recovered: true } as  IPolling)),
            catchError(err => {
              console.error(this.translate.instant('container.initializing_error'), err);
              return of({ container: null, recovered: false } as  IPolling);
            })
          );
        } else {
          return this.containerApiService.getContainer(this.sessionCourseOrCourse?.id).pipe(
            map(container => ({ container, recovered: false } as  IPolling)),
            catchError(err => {
              console.error(this.translate.instant('container.fetch_error'), err);
              this.recovering = true;
              return of({ container: null, recovered: false } as  IPolling);
            })
          );
        }
      })
    )
    .subscribe(
      (pollingResult: IPolling) => {
        let container = pollingResult.container
        if (pollingResult.recovered && this.recovering) {
          this.recovering = false;
          return;
        }

        if (container) {
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
    )
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
