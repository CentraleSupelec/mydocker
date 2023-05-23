import { Inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class UserMeService {

  constructor(
    private readonly httClient: HttpClient,
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
  ) { }


  getUserMe(): Observable<string[]> {
    return this.httClient.get<string[]>(`${this.config.back_url}user/roles/`);
  }
}
