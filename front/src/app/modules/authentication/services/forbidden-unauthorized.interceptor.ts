import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { TokenService } from "./token.service";
import { AuthService } from "./auth.service";


@Injectable()
export class ForbiddenUnauthorizedInterceptor implements HttpInterceptor {
  private readonly backUrl: string;

  constructor(
    private readonly tokenService: TokenService,
    private readonly authService: AuthService,
    @Inject(APP_CONFIG) config: IAppConfig
  ) {
    this.backUrl = config.back_url;
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // don't override request if host is not admin-api
    if (!req.url.startsWith(this.backUrl)) {
      return next.handle(req);
    }

    // don't override admin-api authentication request
    if (req.url.startsWith(this.backUrl + 'auth/')) {
      return next.handle(req);
    }

    // add authorization header
    return next.handle(req).pipe(catchError((error) => this.handleAuthError(error)));
  }

  private handleAuthError(error: HttpErrorResponse): Observable<any> {
    // Handle your auth error or rethrow
    if (error.status === 403) {
      console.error('Vous n\'êtes pas autorisé à faire cette action');
    }
    if (error.status === 401) {
      this.authService.signOut('/login');
    }
    return throwError(error);
  }
}
