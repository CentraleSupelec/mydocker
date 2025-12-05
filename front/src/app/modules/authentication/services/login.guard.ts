import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { TokenService } from './token.service';


@Injectable()
export class LoginGuard implements CanActivate {
  constructor(private tokenService: TokenService, private router: Router) {}

  canActivate(): boolean {
    if (this.tokenService.isSignedIn()) {
      this.router.navigate(['/shell']);
      return false;
    }
    return true;
  }
}
