import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { IPermission } from "../../interfaces/permission";
import { Subject } from "rxjs";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { switchMap } from "rxjs/operators";
import { takeUntil } from "rxjs/operators";
import { IAdminCourse } from "../../../admin-course/interfaces/course";
import { CoursePermissionApiService } from "../../services/course-permission-api.service";

@Component({
  selector: 'app-course-permission-dialog',
  templateUrl: './course-permission-dialog.component.html',
  styleUrls: ['./course-permission-dialog.component.css']
})
export class CoursePermissionDialogComponent implements OnInit, OnDestroy {
  permission: IPermission[] = [];
  private readonly refresh$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: IAdminCourse,
    public dialogRef: MatDialogRef<CoursePermissionDialogComponent>,
    private readonly coursePermissionApiService: CoursePermissionApiService,
  ) { }

  ngOnInit(): void {
    this.refresh$
      .pipe(
        switchMap(() => this.coursePermissionApiService.listPermission(
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
    this.coursePermissionApiService.givePermission(
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
    this.coursePermissionApiService.deletePermission(
      permission.id
    ).subscribe(
      () => this.refresh$.next()
    );
  }
}
