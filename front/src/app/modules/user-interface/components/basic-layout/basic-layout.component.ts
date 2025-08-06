import { Component, Inject, OnInit } from "@angular/core";
import { AuthService } from "../../../authentication/services/auth.service";
import { TokenService } from "../../../authentication/services/token.service";
import { APP_CONFIG, IAppConfig, IInformation } from "../../../../app-config";

@Component({
  templateUrl: "./basic-layout.component.html",
  styleUrls: ["./basic-layout.component.scss"],
})
export class BasicLayoutComponent implements OnInit {

  userInfo = '';
  email = '';
  appName: string | undefined = '';
  information: IInformation[] | undefined = [];

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly authService: AuthService,
    protected readonly tokenService: TokenService,
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

}
