import { Component, Inject } from '@angular/core';
import { APP_CONFIG, IAppConfig } from '../../../../app-config';

/**
 * Where the back end sends an LTI launch it refused because it resolved to an administrator
 * account. Static on purpose: the launch carries no session and the URL carries no reason.
 */
@Component({
  selector: 'app-lti-refused',
  templateUrl: './refused.component.html',
})
export class RefusedComponent {
  readonly appName: string;

  constructor(@Inject(APP_CONFIG) config: IAppConfig) {
    this.appName = config.app_name || 'MyDocker';
  }
}
