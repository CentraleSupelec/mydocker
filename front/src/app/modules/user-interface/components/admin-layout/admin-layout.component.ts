import {ChangeDetectorRef, Component, Inject, OnInit} from '@angular/core';
import {APP_CONFIG, IAppConfig} from "../../../../app-config";
import {AuthService} from "../../../authentication/services/auth.service";
import {APP_MODE, AppModeService} from "../../../utils/services/app-mode.service";
import {BehaviorSubject} from "rxjs";
import { TokenService } from "../../../authentication/services/token.service";

@Component({
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent implements OnInit {
  userInfo = '';
  email = '';

  courseRoute = new BehaviorSubject<string>('/admin/courses');
  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly authService: AuthService,
    private readonly appModeService: AppModeService,
    private readonly cd: ChangeDetectorRef,
    private readonly tokenService: TokenService
  ) { }

  ngOnInit() {
    this.appModeService
      .mode
      .subscribe(mode => {
        if (mode === APP_MODE.LTI) {
          this.courseRoute.next('/lti/deep-linking');
          this.cd.detectChanges();
        }
      });
      this.email = this.tokenService.getToken()?.decodedToken?.email ?? '';
      this.userInfo = `
        Username: ${this.tokenService.getToken()?.decodedToken.sub}
        Email: ${this.tokenService.getToken()?.decodedToken.email}
        `;
  }

  logout() {
    this.authService.signOut();
  }
}
