import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { VerificationService } from "../../services/verification.service";
import { catchError, mergeMap } from "rxjs/operators";
import { TokenService } from "../../services/token.service";
import { LoginResponse, OidcSecurityService } from "angular-auth-oidc-client";
import { SnackNotificationService } from "../../../utils/snack-notification/snack-notification.service";
import { LocalStorageService } from "../../../utils/services/local-storage.service";
import { MagicLinkService } from "../../services/magic-link.service";
import { of } from "rxjs";

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
    private readonly snackNotificationService: SnackNotificationService,
    private readonly localStorageService: LocalStorageService,
    private readonly magicLinkService: MagicLinkService
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
              const {isAuthenticated, accessToken, errorMessage} = loginResponse;
              if (isAuthenticated) {
                return this.verificationService.validateOidcToken(accessToken);
              } else {
                this.snackNotificationService.push('Impossible de vous authentifier', 'error');
                const error = `Authentication error : ${errorMessage}`;
                this.router.navigateByUrl('/login?preventAutoSignIn=true');
                throw new Error(error);
              }
            }));
        } else if (paramMap.has("token")) {
          const token = paramMap.get("token");
          return this.magicLinkService.login(token).pipe(
            mergeMap((res) => {
              localStorage.setItem("access_token", res.token);
              return of(res.token);
            }),
            catchError((error) => {
              this.router.navigate(['/login'], {
                state:{error_message: 'Lien expiré'}
              })
              throw new Error(error);
            })
          );
        }
        this.router.navigate(["/"]);
        throw new Error("There is no ticket and no code in param map");
      }),
      mergeMap(token => this.tokenService.loadToken(token)),
    ).subscribe(
      () => this.router.navigateByUrl(
        decodeURIComponent(this.redirectTo ?? this.localStorageService.sessionGetAndRemove("redirectTo") ?? "/" ))
    );
  }
}
