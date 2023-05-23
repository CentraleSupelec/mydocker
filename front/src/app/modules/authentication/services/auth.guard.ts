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
    this.router.navigate(['login'], {queryParams: {redirectTo: url}});
    return false;
  }
}
