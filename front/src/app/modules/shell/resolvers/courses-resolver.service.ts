import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { UserCourseApiService } from "../services/user-course-api.service";
import { IBasicCourseWithSession } from "../interfaces/course";

// null means the load failed; the interceptor has already handled a 401 before this catch.
@Injectable({
  providedIn: 'root'
})
export class CourseSessionsResolver implements Resolve<IBasicCourseWithSession[] | null> {
  constructor(
    private readonly courseApiService: UserCourseApiService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IBasicCourseWithSession[] | null> {
    return this.courseApiService.getUserCourses().pipe(catchError(() => of(null)));
  }
}
