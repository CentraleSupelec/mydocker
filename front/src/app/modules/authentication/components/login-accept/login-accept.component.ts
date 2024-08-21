import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { VerificationService } from "../../services/verification.service";
import { mergeMap } from "rxjs/operators";
import { TokenService } from "../../services/token.service";
import { LoginResponse, OidcSecurityService } from "angular-auth-oidc-client";

@Component({
  selector: "app-login-accept",
  templateUrl: "./login-accept.component.html",
  styleUrls: ["./login-accept.component.css"],
})
export class LoginAcceptComponent implements OnInit {
  private redirectTo: string | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly verificationService: VerificationService,
    private readonly tokenService: TokenService,
    private readonly router: Router,
    private readonly oidcSecurityService: OidcSecurityService,
  ) {
  }

  ngOnInit(): void {
    this.route.queryParamMap.pipe(
      mergeMap(paramMap => {
        this.redirectTo = paramMap.get("redirectTo");
        if (paramMap.has("ticket")) {
          return this.verificationService.validateCasTicket(paramMap.get("ticket"), this.redirectTo);
        } else if (paramMap.has("code")) {
          return this.oidcSecurityService
            .checkAuth()
            .pipe(mergeMap((loginResponse: LoginResponse) => {
              const {isAuthenticated, accessToken} = loginResponse;
              if (isAuthenticated) {
                return this.verificationService.validateOidcToken(accessToken, this.redirectTo);
              }
              throw new Error("Code is defined but still unauthenticated");
            }));
        }
        throw new Error("There is no ticket and no code in param map");
      }),
      mergeMap(token => this.tokenService.loadToken(token)),
    ).subscribe(
      () => this.router.navigateByUrl(decodeURIComponent(this.redirectTo || "/")),
    );
  }
}
