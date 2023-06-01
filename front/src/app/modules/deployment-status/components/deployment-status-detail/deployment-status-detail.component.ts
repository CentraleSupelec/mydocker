import { Component, OnInit, ViewChild } from '@angular/core';
import { IDeploymentStatus } from "../../deployment-status";
import { merge, Subject } from "rxjs";
import { DeploymentStatusApiService } from "../../services/deployment-status-api.service";
import { MatDialog } from "@angular/material/dialog";
import { mergeMap } from "rxjs/operators";
import { DeploymentStatusDetailDialogComponent } from "../deployment-status-detail-dialog/deployment-status-detail-dialog.component";
import { MatPaginator } from "@angular/material/paginator";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";

@Component({
  templateUrl: './deployment-status-detail.component.html',
  styleUrls: ['./deployment-status-detail.component.css'],
})
export class DeploymentStatusDetailComponent implements OnInit {
  deploymentsStatus: IDeploymentStatus[] = [];
  deploymentsSize: number | undefined;
  displayedColumns = ['status', 'updatedOn', 'action'];
  polling = false;
  refresh$ = new Subject<void>();

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator | undefined;
  private readonly stopPolling$: Subject<void> = new Subject<void>();

  constructor(
    private readonly deploymentStatusApiService: DeploymentStatusApiService,
    private readonly matDialog: MatDialog,
    private readonly toasterService: ObservableSnackNotificationService,
  ) { }

  ngOnInit(): void {
    if (this.paginator) {
      merge(
          this.refresh$,
          this.paginator.page
      ).pipe(
        mergeMap(() => {
          return this.deploymentStatusApiService.getDeploymentsStatus(this.paginator?.pageIndex, this.paginator?.pageSize)
        })
      ).subscribe(
        page => {
          this.deploymentsStatus = page.content
          this.deploymentsSize = page.totalElements
          if (this.deploymentsStatus[0]?.status !== 'RUNNING') {
            this.stopPolling$.next()
            this.polling = false;
          }
        }
      )
      this.paginator.pageSize = 25
      this.paginator.pageIndex = 0
      this.refresh$.next()
    }
  }

  openDetailDialog(element: IDeploymentStatus) {
    this.matDialog.open(DeploymentStatusDetailDialogComponent, { data: element});
  }

  launchDeployment() {
    this.toasterService.toastWithCallback(
      this.deploymentStatusApiService.launchDeployment(),
      "Le déploiement a bien été lancé",
      "Un déploiment est déjà en cours",
      () => {
        this.refresh$.next();
      }
    )
  }
}
