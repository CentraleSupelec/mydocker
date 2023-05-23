import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { UserCourseApiService } from "../services/user-course-api.service";
import { IBasicCourseWithSession } from "../interfaces/course";

@Injectable({
  providedIn: 'root'
})
export class CourseSessionsResolver implements Resolve<IBasicCourseWithSession[]> {
  constructor(
    private readonly courseApiService: UserCourseApiService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IBasicCourseWithSession[]> {
    return this.courseApiService.getUserCourses();
  }
}
