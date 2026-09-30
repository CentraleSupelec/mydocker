import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { UserCourseApiService } from "../services/user-course-api.service";
import { ISession } from "../interfaces/session";

// null means the load failed; the interceptor has already handled a 401 before this catch.
@Injectable({
  providedIn: 'root'
})
export class UserCourseSessionsResolver implements Resolve<ISession[] | null> {
  constructor(
    private readonly courseApiService: UserCourseApiService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ISession[] | null> {
    return this.courseApiService.getUserCourseSessions().pipe(catchError(() => of(null)));
  }
}
