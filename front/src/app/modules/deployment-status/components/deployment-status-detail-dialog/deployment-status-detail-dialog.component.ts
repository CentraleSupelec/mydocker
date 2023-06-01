import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { IDeploymentStatus } from "../../deployment-status";
import { DeploymentStatusApiService } from "../../services/deployment-status-api.service";
import { interval, Subject } from "rxjs";
import { mergeMap, takeUntil } from "rxjs/operators";

@Component({
  templateUrl: './deployment-status-detail-dialog.component.html',
  styleUrls: ['./deployment-status-detail-dialog.component.css']
})
export class DeploymentStatusDetailDialogComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: IDeploymentStatus,
    private readonly dialogRef: MatDialogRef<DeploymentStatusDetailDialogComponent>,
    private readonly deploymentStatusApiService: DeploymentStatusApiService,
  ) {}

  close() {
    this.dialogRef.close();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  ngOnInit(): void {
    if (this.data.status === 'RUNNING') {
      interval(1000)
        .pipe(
          mergeMap(() => this.deploymentStatusApiService.getDeploymentStatus(this.data.id)),
          takeUntil(this.destroy$)
        )
        .subscribe(
          data => this.data = data
        )
    }
  }
}
