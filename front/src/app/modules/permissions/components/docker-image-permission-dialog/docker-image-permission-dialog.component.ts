import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { IDockerImage } from "../../../admin-docker-image/interfaces/docker-image";
import { DockerImagePermissionApiService } from "../../services/docker-image-permission-api.service";
import { IPermission } from "../../interfaces/permission";
import { Subject } from "rxjs";
import { switchMap } from "rxjs/operators";
import { takeUntil } from "rxjs/operators";

@Component({
  selector: 'app-docker-image-permission-dialog',
  templateUrl: './docker-image-permission-dialog.component.html',
  styleUrls: ['./docker-image-permission-dialog.component.css']
})
export class DockerImagePermissionDialogComponent implements OnInit, OnDestroy {
  permission: IPermission[] = [];
  private readonly refresh$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: IDockerImage,
    public dialogRef: MatDialogRef<DockerImagePermissionDialogComponent>,
    private readonly dockerImagePermissionApiService: DockerImagePermissionApiService,
  ) { }

  ngOnInit(): void {
    this.refresh$
      .pipe(
        switchMap(() => this.dockerImagePermissionApiService.listPermission(
          this.data.id
        )),
        takeUntil(this.destroy$)
      )
      .subscribe(
        p => this.permission = p
      );
    this.refresh$.next();
  }

  close() {
    this.dialogRef.close();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  save(permission: IPermission) {
    this.dockerImagePermissionApiService.givePermission(
      this.data.id, {
        userId: permission.user.id,
        type: permission.type,
        id: permission.id
      }
    ).subscribe(
      () => this.refresh$.next()
    );
  }

  delete(permission: IPermission) {
    this.dockerImagePermissionApiService.deletePermission(
      permission.id
    ).subscribe(
      () => this.refresh$.next()
    );
  }
}
