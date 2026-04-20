import { Component, Inject, OnInit } from "@angular/core";
import { AuthService } from "../../../authentication/services/auth.service";
import { TokenService } from "../../../authentication/services/token.service";
import { APP_CONFIG, IAppConfig, IInformation, Language, LANGUAGES } from "../../../../app-config";
import { TranslateService } from "@ngx-translate/core";

@Component({
  templateUrl: "./basic-layout.component.html",
  styleUrls: ["./basic-layout.component.scss"],
})
export class BasicLayoutComponent implements OnInit {

  userInfo = '';
  email = '';
  appName: string | undefined = '';
  information: IInformation[] | undefined = [];
  languages = LANGUAGES;

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly authService: AuthService,
    protected readonly tokenService: TokenService,
    private readonly translate: TranslateService
  ) {
  }

  ngOnInit(): void {
    this.email = this.tokenService.getToken()?.decodedToken?.email ?? '';
    this.appName = this.config.app_name;
    this.information = this.config.information;
    this.userInfo = `
        Username: ${this.tokenService.getToken()?.decodedToken.sub}
        Email: ${this.tokenService.getToken()?.decodedToken.email}
        `;
  }

  logout() {
    this.authService.signOut();
  }

  get currentFlag(): string {
    const language = this.currentLang;
    return this.languages.find(l => l.code === language)?.flag || '🌐';
  }

  switchLang(language: string) {
    this.translate.use(language);
    localStorage.setItem('language', language);
  }

  get currentLang(): Language {
    return ((this.translate.currentLang || this.translate.getDefaultLang()) as Language);
  }
}
