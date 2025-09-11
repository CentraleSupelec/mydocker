import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { IBasicCourse, IBasicCourseWithSession } from "../interfaces/course";
import { ISession } from "../interfaces/session";

@Injectable({
  providedIn: 'root'
})
export class UserCourseApiService {

  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httpClient: HttpClient,
  ) { }

  getUserCourses(): Observable<IBasicCourseWithSession[]> {
    return this.httpClient.get<IBasicCourseWithSession[]>(`${this.config.back_url}courses/joined`);
  }

  getUserCourseSessions(): Observable<ISession[]> {
    return this.httpClient.get<ISession[]>(`${this.config.back_url}courses-sessions/joined`);
  }

  getCourseInformationByLink(link: string | null): Observable<IBasicCourse> {
    return this.httpClient.get<IBasicCourse>(`${this.config.back_url}courses/${link}`);
  }

  joinCourse(courseId: number): Observable<IBasicCourse> {
    return this.httpClient.put<IBasicCourse>(`${this.config.back_url}courses/${courseId}/join`, null);
  }
}
