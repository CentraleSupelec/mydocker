import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot } from '@angular/router';
import { TokenService } from './token.service';
import { AuthGuard } from './auth.guard';
import { NgxPermissionsGuard } from 'ngx-permissions';


@Injectable()
export class AutologinGuard implements CanActivate {

  constructor(private tokenService: TokenService,
              private authGuard: AuthGuard,
              private ngxPermissionsGuard: NgxPermissionsGuard) {
  }

  async canActivate(route: ActivatedRouteSnapshot,
                    state: RouterStateSnapshot): Promise<boolean> {
    const idToken = route.queryParamMap.get('idtoken');
    if (idToken) {
      await this.tokenService.loadToken(idToken as string).toPromise();
    }
    return (await Promise.all([
      this.authGuard.canActivate(route, state), this.ngxPermissionsGuard.canActivate(route, state)
    ])).every(Boolean);
  }
}
