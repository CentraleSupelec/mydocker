import { Inject, Injectable } from "@angular/core";
import { HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { TokenService } from "./token.service";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { Observable } from "rxjs";

export function setAuthorizationHeaderOnRequest(headers: HttpHeaders, token: string): HttpHeaders {
  return headers.set('Authorization', 'Bearer ' + token);
}

@Injectable()
export class AddTokenInterceptor implements HttpInterceptor {
  private readonly backUrl: string;

  constructor(
    private readonly tokenService: TokenService,
    @Inject(APP_CONFIG) config: IAppConfig,
  ) {
    this.backUrl = config.back_url;
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // don't override request if host is not backUrl
    if (!req.url.startsWith(this.backUrl)) {
      return next.handle(req);
    }

    // don't override admin-api authentication request
    if (req.url.startsWith(`${this.backUrl}auth/`)) {
      return next.handle(req);
    }

    // don't override if no token is available
    const token = this.tokenService.getToken();
    if (!token) {
      return next.handle(req);
    }

    const authRequest = req.clone({
      headers: setAuthorizationHeaderOnRequest(req.headers, token.encodedToken)
    });

    // add authorization header
    return next.handle(authRequest);
  }
}
