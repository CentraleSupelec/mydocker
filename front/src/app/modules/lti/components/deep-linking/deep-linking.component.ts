import {ChangeDetectorRef, Component, ElementRef, Inject, OnInit, ViewChild} from '@angular/core';
import {Subject} from 'rxjs';
import {IAdminCourse} from '../../../admin-course/interfaces/course';
import {LtiApiService} from '../../services/lti-api.service';
import {APP_CONFIG, IAppConfig} from '../../../../app-config';
import {APP_MODE, AppModeService} from "../../../utils/services/app-mode.service";

@Component({
  selector: 'app-deep-linking',
  templateUrl: './deep-linking.component.html',
  styleUrls: ['./deep-linking.component.css']
})
export class DeepLinkingComponent implements OnInit {

  readonly stopContainerPolling$ = new Subject<void>();
  @ViewChild('jwtForm') jwtForm!: ElementRef;
  loading = false;
  JWT?: string;
  deepLinkingUrl?: string;

  constructor(
    private readonly ltiApiService: LtiApiService,
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly cd: ChangeDetectorRef,
    private readonly appModeService: AppModeService
    ) {
  }

  ngOnInit() {
    this.appModeService.setMode(APP_MODE.LTI);
  }

  choose({ link }: IAdminCourse) {
    const url = `${ this.config.front_url}/shell/join/${ link }`;
    this.ltiApiService
      .deepLink(url)
      .subscribe(({ deepLinkingUrl, JWT }) => {
        this.JWT = JWT;
        this.deepLinkingUrl = deepLinkingUrl;
        this.cd.detectChanges();
        this.jwtForm.nativeElement.submit();
      })
    ;
  }
}
