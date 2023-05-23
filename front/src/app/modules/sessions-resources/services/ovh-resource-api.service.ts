import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { IOvhResource } from "../interfaces/ovh-resource";

@Injectable({
  providedIn: 'root'
})
export class OvhResourceApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {}

  getResources(): Observable<IOvhResource[]> {
    return this.httClient.get<IOvhResource[]>(`${this.config.back_url}admin/scale/ovh-resources`);
  }
}
