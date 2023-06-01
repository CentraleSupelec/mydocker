import {ChangeDetectorRef, Component, Inject, OnInit} from '@angular/core';
import {APP_CONFIG, IAppConfig} from "../../../../app-config";
import {AuthService} from "../../../authentication/services/auth.service";
import {APP_MODE, AppModeService} from "../../../utils/services/app-mode.service";
import {BehaviorSubject} from "rxjs";

@Component({
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent implements OnInit {

  courseRoute = new BehaviorSubject<string>('/admin/courses');
  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private readonly authService: AuthService,
    private readonly appModeService: AppModeService,
    private readonly cd: ChangeDetectorRef
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
  }

  logout() {
    this.authService.signOut();
    let params = new URLSearchParams();
    params.set("service", `${this.config.front_url}/login`)
    window.location.href = `${this.config.cas.logout_url}?${params.toString()}`
  }
}
