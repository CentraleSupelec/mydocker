import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { AdminCoursesApiService } from "../services/admin-courses-api.service";
import { IAdminCourse } from "../interfaces/course";

@Injectable({
  providedIn: 'root'
})
export class AdminCourseResolver implements Resolve<IAdminCourse> {
  constructor(
    private readonly adminCoursesApiService: AdminCoursesApiService
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IAdminCourse> {
    const id = parseInt(<string>route.paramMap.get('id'), 10);
    return this.adminCoursesApiService.getCourse(id);
  }
}
