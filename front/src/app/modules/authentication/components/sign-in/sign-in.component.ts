import { Component, Inject, OnInit } from "@angular/core";
import { APP_CONFIG, IAppConfig, IInformation } from "../../../../app-config";
import { ActivatedRoute, Router } from "@angular/router";
import { NavigationService } from "../../../utils/services/navigation.service";
import { OidcSecurityService } from "angular-auth-oidc-client";
import { TokenOrigin } from "../../interfaces/jwt-token";
import { LocalStorageService } from "../../../utils/services/local-storage.service";
import { skipWhile } from "rxjs/operators";
import { MagicLinkService } from "../../services/magic-link.service";
import { UserCourseApiService } from "src/app/modules/shell/services/user-course-api.service";
import { TokenService } from "../../services/token.service";
import { Location } from "@angular/common";
import { IBasicCourseWithSession } from "src/app/modules/shell/interfaces/course";

@Component({
  selector: 'app-sign-in',
  templateUrl: './sign-in.component.html',
  styleUrls: ['./sign-in.component.css']
})
export class SignInComponent implements OnInit {
  private redirectTo: string = "/";
  showButtons= false;
  showInformation = false;
  isCasLoginEnabled = false;
  isOIDCLoginEnabled = false;
  information!: Array<IInformation>;
  appName: string | undefined = undefined;
  isMagicLink = false;
  emailSent = false;
  courseUuid: string | null = null;
  email: string = '';
  courseExternalAccessActivated = false;
  errorMessage: string | undefined = undefined;
  magicLinkExpirationInMinutes: number | undefined = undefined

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private route: ActivatedRoute,
    private readonly navigationService: NavigationService,
    private readonly oidcSecurityService: OidcSecurityService,
    private readonly localStorageService: LocalStorageService,
    private readonly magicLinkService: MagicLinkService,
    private readonly userCourseApiService: UserCourseApiService,
    private readonly tokenService: TokenService,
    private router: Router,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.isOIDCLoginEnabled = !!(this.config.oidc_authority && this.config.oidc_authority && this.config.oidc_scope);
    this.isCasLoginEnabled = !!this.config.cas.login_url;
    this.showButtons = this.config.auto_login !== TokenOrigin.CAS
      && this.config.auto_login !== TokenOrigin.OIDC
      && (this.isOIDCLoginEnabled || this.isCasLoginEnabled);
    this.information = this.config.information || [];
    this.appName = this.config.app_name;
    this.magicLinkExpirationInMinutes = this.config.magic_link_expiration_in_minutes;
    this.showInformation = this.information && this.information.length > 0;
    this.isMagicLink = this.route.snapshot.data['isMagicLink'] ?? false;
    this.courseUuid = this.route.snapshot.paramMap.get('courseUuid');
    this.errorMessage = (this.location.getState() as { error_message?: string })?.error_message;

    if (this.isMagicLink && this.tokenService.isSignedIn()) {
      this.userCourseApiService.getUserCourses().subscribe(
        (userCourses: IBasicCourseWithSession[]) => {
          const course = userCourses.find(userCourse => userCourse.uuid == this.courseUuid)
          if (course) {
            this.router.navigate(['/shell'], {queryParams: {course_id: course.id}})
          } else {
            this.router.navigate(['/shell'], {queryParams: {
              error_message: "Le cours n'a pas été trouvé"
            }})
          }
        }
      )
    }

    if (this.courseUuid !== null) {
      this.userCourseApiService.getIsExternalAccessActivated(this.courseUuid).subscribe({
        next: (externalAccessActivated) => {
          if (!externalAccessActivated) {
            this.router.navigate(['/login'], {
              state: { error_message: "L'accès par lien de connexion est désactivé pour ce cours" }
            });
          }
        },
        error: (err) => {
          console.error('Error fetching external access status:', err);
          this.router.navigate(['/login'], {
            state: { error_message: "Impossible de vérifier l'accès pour ce cours" }
          });
        }
      });
    }

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

  get containerDimension(): string {
    const itemCount = (this.isCasLoginEnabled ? 1 : 0)
      + (this.config.oidc_idps?.length || 0)
      + (this.information?.length || 0);
    return (35 + itemCount * 5) + 'vh';
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

  sendMagicLink() {
    this.emailSent = false;
    if (!this.email || !this.courseUuid) {
      return;
    }
    this.magicLinkService.sendMagicLink(this.email, this.courseUuid)
      .subscribe({
        next: () => this.emailSent = true,
        error: () => this.errorMessage = "Une erreur s'est produite lors de l'envoi de l'email"
      }
      );
  }

  dismiss() {
    this.errorMessage = undefined;
  }
}
