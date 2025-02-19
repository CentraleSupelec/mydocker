import { ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { interval, Subject } from "rxjs";
import { ContainerApiService } from "../../services/container-api.service";
import { mergeMap, takeUntil } from "rxjs/operators";
import {
  ContainerStatus,
  ContainerSwarmState,
  ContainerSwarmStateMessages,
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
  @Input() launch: boolean = false;
  @Input() userRedirect: string | undefined = undefined;

  container: IContainer | null = null;
  state: 'ask' | 'loading_init' | 'loading_shutdown' | 'container_created' | 'pending' = 'ask';
  step: number = 0;
  stepMessage: string = '';

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
  ) {
  }
  ngOnChanges(changes: SimpleChanges): void {
    if (changes.launch && this.launch && this.canAskContainer()) {
      this.initGetContainer();
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

  initGetContainer(forceRecreate: boolean = false) {
    this.containerApiService.initGetContainer(this.session?.id, forceRecreate)
      .subscribe(
        () => this.startInitPolling()
      );
  }

  private startInitPolling() {
    this.state = 'loading_init';
    interval(3000).pipe(
      takeUntil(this.stopInitPolling$),
      mergeMap(() => this.containerApiService.getContainer(this.sessionCourseOrCourse?.id))
    ).subscribe(
      container => {
        if (container) {
          this.container = container;
          const index = ContainerSwarmStateOrder.indexOf(ContainerSwarmState[container.state as keyof typeof ContainerSwarmState]);
          this.step =  index * 100 / ContainerSwarmStateOrder.length;
          this.stepMessage = `${ContainerSwarmStateMessages[container.state ?? ContainerSwarmState.UNKNOWN]} (étape ${index + 1}/${ContainerSwarmStateOrder.length})`;
          if (container.status === ContainerStatus.OK || (container.state === ContainerSwarmState.RUNNING && container.status !== ContainerStatus.CHECKING)) {
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
          'Votre environnement va s\'éteindre',
          'Si nécessaire, réinitialisez le minuteur pour retarder l\'extinction.'
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
          this.snackNotificationService.push(`Impossible d'éteindre l'environnement`, 'error');
          this.stopShutdownPolling$.next();
        }
      }
    )
  }

  askNewEnvWithConfirmationDialog() {
    this.dialogConfirmService.confirm({
      title: 'Confirmez-vous la demande d\'un nouvel environnement ?',
      text: 'Demander un nouvel environnement supprimera l\'ancien.'
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
      || !!this.userPermissions?.[Roles.Admin];
  }

  fetchLogs() {
    this.containerApiService.getLogs(this.sessionCourseOrCourse?.id).subscribe(
      logs => this.openLogDialogService.openDialog(logs)
    )
  }

  deleteEnvWithConfirmationDialog() {
    this.dialogConfirmService
      .confirm({
        text: 'Confirmez-vous l\'extinction de l\'environnement ?',
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
