import { Component, Inject } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import { AuthService } from "../../../authentication/services/auth.service";

@Component({
  templateUrl: './basic-layout.component.html',
  styleUrls: ['./basic-layout.component.scss']
})
export class BasicLayoutComponent {

  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly authService: AuthService,
  ) { }

  logout() {
    this.authService.signOut();
    let params = new URLSearchParams();
    params.set("service", `${this.config.front_url}/login`)
    window.location.href = `${this.config.cas.logout_url}?${params.toString()}`
  }
}
