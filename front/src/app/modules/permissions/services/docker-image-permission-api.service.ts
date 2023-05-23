import { Inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { Observable } from "rxjs";
import { IPermission, IUpdatePermission } from "../interfaces/permission";

@Injectable({
  providedIn: 'root'
})
export class DockerImagePermissionApiService {

  constructor(
    private readonly httpClient: HttpClient,
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
  ) { }

  public givePermission(dockerImageId: number, updatePermission: IUpdatePermission): Observable<void> {
    return this.httpClient.put<void>(`${this.config.back_url}permissions/docker-image/${dockerImageId}`, updatePermission);
  }

  public deletePermission(permissionId: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.config.back_url}permissions/docker-image/${permissionId}`);
  }

  public listPermission(dockerImageId: number): Observable<IPermission[]> {
    return this.httpClient.get<IPermission[]>(`${this.config.back_url}permissions/docker-image/${dockerImageId}`);
  }
}
