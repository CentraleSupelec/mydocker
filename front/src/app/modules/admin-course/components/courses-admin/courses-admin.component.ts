import { Component, Inject } from '@angular/core';
import { IAdminCourse } from '../../interfaces/course';
import { mergeMap, takeUntil } from 'rxjs/operators';
import { interval, Subject } from 'rxjs';
import { IContainer } from '../../../shell/interfaces/container';
import { ClipboardSnackService } from '../../../utils/snack-notification/clipboard-snack.service';
import { NgxPermissionsService } from 'ngx-permissions';
import { AdminContainerApiService } from '../../services/admin-container-api.service';
import { SnackNotificationService } from '../../../utils/snack-notification/snack-notification.service';
import { ConfirmDialogService } from '../../../utils/confirm-dialog/confirm-dialog.service';
import { APP_CONFIG, IAppConfig } from '../../../../app-config';
import { GenerateJoinLinkPipe } from "../../../utils/generate-join-link.pipe";

@Component({
  selector: 'app-courses-admin',
  templateUrl: './courses-admin.component.html',
  styleUrls: ['./courses-admin.component.css']
})
export class CoursesAdminComponent {

  container: IContainer | null = null;
  expandedElement: IAdminCourse | null = null;
  readonly stopContainerPolling$ = new Subject<void>();
  askContainer = false;
  constructor(
    private readonly clipboardSnackService: ClipboardSnackService,
    private readonly permissionsService: NgxPermissionsService,
    private readonly adminContainerApiService: AdminContainerApiService,
    private readonly snackNotificationService: SnackNotificationService,
    private readonly dialogConfirmService: ConfirmDialogService,
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly generateJoinLinkPipe: GenerateJoinLinkPipe,
    ) { }

  private adminContainerPolling(element: IAdminCourse, forceRecreate: boolean) {
    this.adminContainerApiService.initGetContainer(element.id, forceRecreate)
      .pipe(
        mergeMap(() => interval(3000)),
        mergeMap(() => this.adminContainerApiService.getContainer(element.id)),
        takeUntil(this.stopContainerPolling$)
      )
      .subscribe(
        (container: IContainer) => {
          if (container) {
            this.container = container;
            this.stopContainerPolling$.next();
          }
        },
        error => {
          console.error(error);
          this.stopContainerPolling$.next();
          this.askContainer = false;
          this.snackNotificationService
            .push("Nous avons rencontré un problème lors de la création de l'environnement ...", 'error');
        }
      )
  }

  copyLink($event: Event, link: string) {
    $event.stopPropagation();
    this.clipboardSnackService.copyWithNotification(this.generateJoinLinkPipe.transform(link));
  }

  askAdminContainer($event: MouseEvent, element: IAdminCourse) {
    this.stopContainerPolling$.next()
    this.expandedElement = this.expandedElement === element ? null : element;
    this.container = null;
    this.askContainer = true;
    if (this.expandedElement) {
      this.adminContainerPolling(element, false);
    }
  }

  forceRecreateAdminContainer(element: IAdminCourse) {
    this.dialogConfirmService.confirm({
      title: 'Confirmez-vous la demande d\'un nouvel environnement ?',
      text: 'Si vous venez d\'avoir les accès et que ceux-ci ne fonctionnent pas, attendez quelques minutes avant d\'en demander un nouveau.'
    }).subscribe(
      (confirm:boolean) => {
        if(confirm) {
          this.container = null;
          this.adminContainerPolling(element,true);
        }
      }
    )
  }

}
