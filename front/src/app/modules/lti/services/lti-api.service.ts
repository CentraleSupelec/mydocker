import { Inject, Injectable } from '@angular/core';
import { APP_CONFIG, IAppConfig } from '../../../app-config';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IDeepLinkingResult } from '../interfaces/deep-linking-result';

@Injectable({
  providedIn: 'root'
})
export class LtiApiService {
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly httClient: HttpClient,
  ) {}

  register(openidConfiguration: string, registrationToken: string): Observable<void> {
    return this.httClient.get<void>(`${this.config.back_url}lti/register`, {
      params: {
        openid_configuration: openidConfiguration,
        registration_token: registrationToken,
      }
    })
  }

  deepLink(url: string): Observable<IDeepLinkingResult> {
    return this.httClient.post<IDeepLinkingResult>(`${this.config.back_url}lti/deep-linking`, {
      url,
    });
  }

}
