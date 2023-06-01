import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { ISessionWithResources } from "../interfaces/session-with-resources";

@Injectable({
  providedIn: 'root'
})
export class SessionWithResourcesApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {}

  getSessions(sessionStartDate: number): Observable<ISessionWithResources[]> {
    return this.httClient.get<ISessionWithResources[]>(`${this.config.back_url}admin/scale/session`, {
      params: {
        startTime: sessionStartDate
      }
    });
  }

  getSession(sessionId: number): Observable<ISessionWithResources> {
    return this.httClient.get<ISessionWithResources>(`${this.config.back_url}admin/scale/session/${sessionId}`);
  }

  updateSession(sessionId: number, session: ISessionWithResources): Observable<void> {
    return this.httClient.put<void>(`${this.config.back_url}admin/scale/session/${sessionId}`, session);
  }
}
