import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { IContainer } from "../../shell/interfaces/container";

@Injectable({
  providedIn: 'root'
})
export class AdminContainerApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {}

  initGetContainer(courseId: number | undefined, forceRecreate: boolean): Observable<void> {
    return this.httClient.post<void>(`${this.config.back_url}docker/initAdminContainer/${courseId}`, {},{
      params: {
        forceRecreate: forceRecreate
      }
    });
  }

  getContainer(courseId: number | undefined): Observable<IContainer> {
    return this.httClient.get<IContainer>(`${this.config.back_url}docker/admin/${courseId}`);
  }
}
