import { Inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class MagicLinkService {

  constructor(
    private readonly httpClient: HttpClient,
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
  ) { }

  sendMagicLink(email: string, courseUuid: string): Observable<any> {
    return this.httpClient.post<any>(
      `${this.config.back_url}auth/magic-link`,
      { email, courseUuid }
    );
  }

  login(token: string| null): Observable<any> {
    return this.httpClient.post<any>(
      `${this.config.back_url}auth/magic-link/login`,
      { token }
    );
  }
}
