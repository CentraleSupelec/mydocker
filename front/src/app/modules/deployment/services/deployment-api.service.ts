import { Inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { IDeployment, IUpdateDeployment } from "../interfaces/deployment";
import { Observable } from "rxjs";
import { ISession } from "../../shell/interfaces/session";
import { IRegionWorkerWithSession } from "../interfaces/region-worker";

@Injectable({
  providedIn: 'root'
})
export class DeploymentApiService {

  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) { }

  getDeployments(): Observable<IDeployment[]> {
    return this.httClient.get<IDeployment[]>(
      `${this.config.back_url}admin/deployment`
    )
  }

  getDeployment(id: number): Observable<IDeployment> {
    return this.httClient.get<IDeployment>(
      `${this.config.back_url}admin/deployment/${id}`
    )
  }

  createDeployment(deployment: IUpdateDeployment): Observable<void> {
    return this.httClient.post<void>(
      `${this.config.back_url}admin/deployment`, deployment
    )
  }

  updateDeployment(deploymentId: number, deployment: IUpdateDeployment): Observable<void> {
    return this.httClient.put<void>(
      `${this.config.back_url}admin/deployment/${deploymentId}`, deployment
    )
  }

  deleteDeployment(deploymentId: number): Observable<void> {
    return this.httClient.delete<void>(
      `${this.config.back_url}admin/deployment/${deploymentId}`
    )
  }

  getSessionToLaunch(): Observable<ISession[]> {
    return this.httClient.get<ISession[]>(
      `${this.config.back_url}admin/deployment/sessions/launch`
    )
  }

  getSessionToClean(): Observable<ISession[]> {
    return this.httClient.get<ISession[]>(
      `${this.config.back_url}admin/deployment/sessions/clean`
    )
  }

  getRegionWorkerFromSession(sessionId: number): Observable<IRegionWorkerWithSession[]> {
    return this.httClient.get<IRegionWorkerWithSession[]>(
      `${this.config.back_url}admin/deployment/worker/${sessionId}`
    )
  }

  getRegionWorkerToClean(startDateTime: number): Observable<IRegionWorkerWithSession[]> {
    return this.httClient.get<IRegionWorkerWithSession[]>(
      `${this.config.back_url}admin/deployment/worker/clean`, {
        params: {
          startTime: startDateTime
        }
      })
  }
}
