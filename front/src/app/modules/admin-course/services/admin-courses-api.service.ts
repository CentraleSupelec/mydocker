import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient, HttpParams } from "@angular/common/http";
import { CourseStatus, IAdminCourse, IAdminUpdateCourse } from "../interfaces/course";
import { Observable } from "rxjs";
import { IPageResponse } from "../../utils/page";

@Injectable({
  providedIn: 'root'
})
export class AdminCoursesApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) { }

  getCourses(
    search: string,
    status: string[],
    page: number | undefined,
    limit: number | undefined,
    sort: string | undefined,
    direction: 'asc' | 'desc' | undefined,
  ): Observable<IPageResponse<IAdminCourse>> {
    const params = new HttpParams()
      .append('size', limit ? limit.toString() : '')
      .append('page', page? page.toString(): '')
      .append('sort', `${sort},${direction}`)
      .append('search', search)
      .append('status', status.join(', '))
    return this.httClient.get<IPageResponse<IAdminCourse>>(`${this.config.back_url}admin/courses/`, {
      params: params
    });
  }

  getCourse(id: number): Observable<IAdminCourse> {
    return this.httClient.get<IAdminCourse>(`${this.config.back_url}admin/courses/${id}`);
  }

  updateCourse(id: number, course: IAdminUpdateCourse): Observable<IAdminCourse> {
    return this.httClient.put<IAdminCourse>(`${this.config.back_url}admin/courses/${id}`, course);
  }

  createCourse(course: IAdminUpdateCourse): Observable<IAdminCourse> {
    return this.httClient.post<IAdminCourse>(`${this.config.back_url}admin/courses`, course);
  }
}
