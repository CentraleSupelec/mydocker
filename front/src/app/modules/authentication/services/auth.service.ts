import { Inject, Injectable } from "@angular/core";
import { TokenService } from "./token.service";
import { Router } from "@angular/router";
import { APP_CONFIG, IAppConfig } from "../../../app-config";
import { TokenOrigin } from "../interfaces/jwt-token";
import { OidcSecurityService } from "angular-auth-oidc-client";

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(
    private tokenService: TokenService,
    private router: Router,
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly oidcSecurityService: OidcSecurityService,
  ) {
  }

  public signOut(targetUrl?: string) {
    const tokenOrigin = this.tokenService.getToken()?.decodedToken.origin;
    this.tokenService.cleanStorage();
    if (targetUrl) {
      this.router.navigateByUrl(targetUrl);
    } else {
      switch (tokenOrigin) {
        case TokenOrigin.CAS:
          let params = new URLSearchParams();
          params.set("service", `${this.config.front_url}/login`);
          window.location.href = `${this.config.cas.logout_url}?${params.toString()}`;
          break;
        case TokenOrigin.OIDC:
          this.oidcSecurityService.logoff();
          break;
      }
    }
  }
}
