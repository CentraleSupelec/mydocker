import { Injectable } from '@angular/core';
import { TokenService } from "./token.service";
import { Router } from "@angular/router";

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(
    private tokenService: TokenService,
    private router: Router,
  ) {
  }

  public signOut(targetUrl?: string) {
    this.tokenService.cleanStorage();
    if (targetUrl) {
      this.router.navigateByUrl(targetUrl);
    }
  }
}
