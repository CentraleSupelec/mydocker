import { Component, Inject, OnInit } from "@angular/core";
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import { ActivatedRoute } from "@angular/router";
import { NavigationService } from "../../../utils/services/navigation.service";
import { OidcSecurityService } from "angular-auth-oidc-client";
import { TokenOrigin } from "../../interfaces/jwt-token";
import { LocalStorageService } from "../../../utils/services/local-storage.service";
import { skipWhile } from "rxjs/operators";

@Component({
  selector: 'app-sign-in',
  templateUrl: './sign-in.component.html',
  styleUrls: ['./sign-in.component.css']
})
export class SignInComponent implements OnInit {
  private redirectTo: string = "/";
  showButtons= false;
  isCasLoginEnabled = false;
  isOIDCLoginEnabled = false;

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private route: ActivatedRoute,
    private readonly navigationService: NavigationService,
    private readonly oidcSecurityService: OidcSecurityService,
    private readonly localStorageService: LocalStorageService,
  ) {}

  ngOnInit(): void {
    this.isOIDCLoginEnabled = !!(this.config.oidc_authority && this.config.oidc_authority && this.config.oidc_scope);
    this.isCasLoginEnabled = !!this.config.cas.login_url;
    this.showButtons = this.config.auto_login !== TokenOrigin.CAS
      && this.config.auto_login !== TokenOrigin.OIDC
      && (this.isOIDCLoginEnabled || this.isCasLoginEnabled);
    this.route.queryParamMap
      .pipe(
        // To avoid handling paramMap when it has not been initialized. See https://github.com/angular/angular/issues/12157#issuecomment-756379506
        skipWhile(() => this.route.component === null)
      )
      .subscribe(
      paramMap => {
        if (paramMap.has('redirectTo')) {
          this.redirectTo = encodeURIComponent(paramMap.get('redirectTo') as string);
        }
        if (paramMap.get('preventAutoSignIn') === 'true') {
          this.showButtons = true;
          return;
        }
        switch (this.config.auto_login) {
          case TokenOrigin.CAS:
            this.redirectToCas();
            break;
          case TokenOrigin.OIDC:
            this.loginOIDC(this.config.oidc_idps?.[0].idp_hint);
            break;
        }
      }
    );
  }

  loginOIDC(hint?: string) {
    const customParams: {[key: string]: string} = {};
    if (hint) {
      customParams["kc_idp_hint"] = hint;
    }
    // TODO : Handle redirectTo when library is upgraded to v16
    this.localStorageService.sessionSet('redirectTo', this.redirectTo);
    this.oidcSecurityService.authorize(undefined, {customParams});
  }

  redirectToCas() {
    let params = new URLSearchParams();
    params.set("service", `${this.config.front_url}/loginAccept?redirectTo=${this.redirectTo}`)
    this.navigationService.navigateTo(`${this.config.cas.login_url}?${params.toString()}`);
  }
}
