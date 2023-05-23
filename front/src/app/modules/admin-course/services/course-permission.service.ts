import { Injectable } from '@angular/core';
import { NgxPermissionsService } from "ngx-permissions";

@Injectable({
  providedIn: 'root'
})
export class CoursePermissionService {

  constructor(
    private readonly permissionService: NgxPermissionsService,
  ) { }

  hasEditPermission(courseId: number | undefined): Promise<boolean> {
    return this.permissionService.hasPermission([
      'course.' + courseId + '.edit',
      'course.' + courseId + '.creator',
      'ROLE_ADMIN'
    ]);
  }
}
