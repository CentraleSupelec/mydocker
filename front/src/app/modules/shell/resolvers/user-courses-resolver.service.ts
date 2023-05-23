import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { UserCourseApiService } from "../services/user-course-api.service";
import { ISession } from "../interfaces/session";

@Injectable({
  providedIn: 'root'
})
export class UserCourseSessionsResolver implements Resolve<ISession[]> {
  constructor(
    private readonly courseApiService: UserCourseApiService,
  ) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ISession[]> {
    return this.courseApiService.getUserCourseSessions();
  }
}
