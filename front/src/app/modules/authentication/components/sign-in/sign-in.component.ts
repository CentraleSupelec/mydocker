import {Component, Inject, OnInit} from '@angular/core';
import {APP_CONFIG, IAppConfig} from "../../../../app-config";
import { ActivatedRoute } from "@angular/router";
import { NavigationService } from "../../../utils/services/navigation.service";

@Component({
  selector: 'app-sign-in',
  templateUrl: './sign-in.component.html',
  styleUrls: ['./sign-in.component.css']
})
export class SignInComponent implements OnInit {
  private redirectTo: string = "/";

  constructor(
    @Inject(APP_CONFIG) private readonly config: IAppConfig,
    private route: ActivatedRoute,
    private readonly navigationService: NavigationService,
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(
      paramMap => {
        if (paramMap.has('redirectTo')) {
          this.redirectTo = encodeURIComponent(paramMap.get('redirectTo') as string);
        }
        this.redirectToCas();
      }
    );
  }

  redirectToCas() {
    let params = new URLSearchParams();
    params.set("service", `${this.config.front_url}/loginAccept?redirectTo=${this.redirectTo}`)
    this.navigationService.navigateTo(`${this.config.cas.login_url}?${params.toString()}`);
  }
}
