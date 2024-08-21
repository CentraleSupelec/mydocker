import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  CanLoad,
  Route,
  Router,
  RouterStateSnapshot
} from '@angular/router';
import { Observable } from 'rxjs';
import { TokenService } from './token.service';


@Injectable()
export class AuthGuard implements CanActivate, CanActivateChild, CanLoad {

  constructor(private tokenService: TokenService,
              private router: Router) {
  }

  canActivate(route: ActivatedRouteSnapshot,
              state: RouterStateSnapshot): boolean {
    return this.checkLogin(state.url);
  }

  canActivateChild(route: ActivatedRouteSnapshot,
                   state: RouterStateSnapshot): Observable<boolean> | Promise<boolean> | boolean {
    return this.checkLogin(state.url);
  }

  canLoad(route: Route): boolean {
    return this.checkLogin(route.path);
  }

  checkLogin(url: string | undefined): boolean {
    if (this.tokenService.isSignedIn()) {
      return true;
    }
    const queryParams: {[key: string]: string} = {};
    if (url) {
      queryParams['redirectTo'] = url;
      const parsedUrl = new URL(window.location.origin + url);
      const urlParams = new URLSearchParams(parsedUrl.search);
      ['code', 'session_state', 'state'].forEach(param => {
        const paramValue = urlParams.get(param);
        if (paramValue) {
          queryParams[param] = paramValue;
        }
      });
    }
    this.router.navigate(['login'], {queryParams});
    return false;
  }
}
