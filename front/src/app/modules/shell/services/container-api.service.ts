import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { IContainer } from "../interfaces/container";
import { ISaveState } from "../interfaces/save-state";
import { IShutdownContainer } from '../interfaces/shutdown-container';
import { IDelayDeletion } from '../interfaces/delay-deletion';

@Injectable({
  providedIn: 'root'
})
export class ContainerApiService {

  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httpClient: HttpClient,
  ) {}

  initGetContainer(sessionId: number | undefined, forceRecreate: boolean): Observable<void> {
    return this.httpClient.post<void>(`${this.config.back_url}docker/initGetContainer/${sessionId}`, {},{
      params: {
        forceRecreate: forceRecreate
      }
    });
  }

  getContainer(courseId: number | undefined): Observable<IContainer> {
    return this.httpClient.get<IContainer>(`${this.config.back_url}docker/container/${courseId}`);
  }

  getSaveState(courseId: number | undefined): Observable<ISaveState> {
    return this.httpClient.get<ISaveState>(`${this.config.back_url}docker/${courseId}/save-state`);
  }

  saveState(courseId: number | undefined): Observable<void> {
    return this.httpClient.post<void>(`${this.config.back_url}docker/saveData/${courseId}`, {});
  }

  shutdownContainer(courseId: number | undefined): Observable<void> {
    return this.httpClient.post<void>(`${this.config.back_url}docker/container/${courseId}/shutdown`, {});
  }

  getShutdownStatus(courseId: number | undefined): Observable<IShutdownContainer> {
    return this.httpClient.get<IShutdownContainer>(`${this.config.back_url}docker/container/${courseId}/shutdown`);
  }

  getLogs(courseId: number | undefined): Observable<string> {
    return this.httpClient.get(`${this.config.back_url}docker/logs/${courseId}`,{
      observe: 'body',
      responseType: 'text'
    });
  }

  delayDeletion(sessionId: number | undefined): Observable<IDelayDeletion> {
    return this.httpClient.post<IDelayDeletion>(`${this.config.back_url}docker/delay-deletion/${sessionId}`, {});
  }
}
