import { Inject, Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { Observable } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class VerificationService {

  constructor(
    private readonly httClient: HttpClient,
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
  ) { }

  validateCasTicket(ticket: string | null, redirectTo: string | null): Observable<string> {
    let serviceUrl = `${this.config.front_url}/loginAccept`;
    if (redirectTo) {
      let params = new URLSearchParams();
      params.set("redirectTo", redirectTo)
      serviceUrl += `?${params.toString()}`;
    }
    return this.httClient.get(`${this.config.back_url}auth/cas/${ticket}?serviceURL=${serviceUrl}`, {
      responseType: 'text'
    });
  }

  validateOidcToken(accessToken: string): Observable<string> {
    return this.httClient.post(`${this.config.back_url}auth/oidc/`, {accessToken}, {
      responseType: 'text'
    });
  }
}
