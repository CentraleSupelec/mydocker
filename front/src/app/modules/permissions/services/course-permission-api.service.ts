import { Inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { IPermission, IUpdatePermission } from "../interfaces/permission";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class CoursePermissionApiService {
  constructor(
    private readonly httpClient: HttpClient,
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
  ) { }

  public givePermission(courseId: number, updatePermission: IUpdatePermission): Observable<void> {
    return this.httpClient.put<void>(`${this.config.back_url}permissions/course/${courseId}`, updatePermission);
  }

  public deletePermission(permissionId: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.config.back_url}permissions/course/${permissionId}`);
  }

  public listPermission(courseId: number): Observable<IPermission[]> {
    return this.httpClient.get<IPermission[]>(`${this.config.back_url}permissions/course/${courseId}`);
  }
}
