import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { IDeploymentStatus } from "../deployment-status";
import { IPageResponse } from "../../utils/page";

@Injectable({
  providedIn: 'root'
})
export class DeploymentStatusApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) { }

  getDeploymentsStatus(page: number | undefined, limit: number | undefined): Observable<IPageResponse<IDeploymentStatus>> {
    let params: HttpParams = new HttpParams()
      .append('size', limit ? limit.toString() : '')
      .append('page', page? page.toString(): '')
    return this.httClient.get<IPageResponse<IDeploymentStatus>>(
      `${this.config.back_url}admin/deployment_status`, {
        params: params,
      })
  }

  getDeploymentStatus(id: number): Observable<IDeploymentStatus> {
    return this.httClient.get<IDeploymentStatus>(
      `${this.config.back_url}admin/deployment_status/${id}`
    )
  }

  launchDeployment(): Observable<void> {
    return this.httClient.post<void>(
      `${this.config.back_url}admin/scale`, {}
    )
  }
}
