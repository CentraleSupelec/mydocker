import { Component, Inject, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { VerificationService } from "../../services/verification.service";
import { catchError, mergeMap } from "rxjs/operators";
import { TokenService } from "../../services/token.service";
import { LoginResponse, OidcSecurityService } from "angular-auth-oidc-client";
import { LocalStorageService } from "../../../utils/services/local-storage.service";
import { MagicLinkService } from "../../services/magic-link.service";
import { EMPTY, Observable, of } from "rxjs";
import { TranslateService } from "@ngx-translate/core";
import { APP_CONFIG, IAppConfig } from "../../../../app-config";

@Component({
  selector: "app-login-accept",
  templateUrl: "./login-accept.component.html",
  styleUrls: ["./login-accept.component.css"],
})
export class LoginAcceptComponent implements OnInit {
  private redirectTo: string | null = null;

  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly route: ActivatedRoute,
    private readonly verificationService: VerificationService,
    private readonly tokenService: TokenService,
    private readonly router: Router,
    private readonly oidcSecurityService: OidcSecurityService,
    private readonly localStorageService: LocalStorageService,
    private readonly magicLinkService: MagicLinkService,
    private readonly translate: TranslateService
  ) {
  }

  ngOnInit(): void {
    this.route.queryParamMap.pipe(
      mergeMap(paramMap => {
        this.redirectTo = paramMap.get("redirectTo");
        if (paramMap.has("ticket")) {
          return this.verificationService.validateCasTicket(paramMap.get("ticket"), this.redirectTo).pipe(
            catchError(() => this.backToSignInWithError())
          );
        } else if (paramMap.has("code")) {
          return this.oidcSecurityService
            .checkAuth()
            .pipe(mergeMap((loginResponse: LoginResponse) => {
              const {isAuthenticated, accessToken, errorMessage} = loginResponse;
              if (isAuthenticated) {
                return this.verificationService.validateOidcToken(accessToken).pipe(
                  catchError(() => this.backToSignInWithError())
                );
              } else {
                console.error(`Authentication error : ${errorMessage}`);
                return this.backToSignInWithError();
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

  /**
   * Sends the user back to the sign-in page, without auto sign-in so it does not loop, with a message
   * asking to contact the support address from the configuration. Completes without a token.
   */
  private backToSignInWithError(): Observable<never> {
    const supportEmail = this.config.support_email;
    const message$: Observable<string> = supportEmail
      ? this.translate.get('sign_in.auth_error', { email: supportEmail })
      : this.translate.get('sign_in.auth_error_no_address');
    return message$.pipe(
      mergeMap(message => {
        this.router.navigate(['/login'], {
          queryParams: { preventAutoSignIn: 'true' },
          state: { error_message: message }
        });
        return EMPTY;
      })
    );
  }
}
