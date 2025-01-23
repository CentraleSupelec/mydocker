import { Component, Input } from '@angular/core';
import { IHttpPortDisplay, IPortDisplay } from "../../admin-course/interfaces/course-display";
import { IContainerPort } from "../../shell/interfaces/container-port";
import { RenderStringService } from "../render-string.service";

@Component({
  selector: 'app-display-custom-container-port',
  templateUrl: './display-custom-container-port.component.html',
  styleUrls: ['./display-custom-container-port.component.css']
})
export class DisplayCustomContainerPortComponent {
  @Input() customDisplay: IPortDisplay | null = null;
  @Input() containerPorts: IContainerPort[] | undefined = [];
  @Input() username: string | undefined = '';
  @Input() ipAddress: string | undefined = '';
  @Input() password: string | undefined = '';
  @Input() userRedirect: string | undefined = '';

  constructor(
    private readonly renderStringService: RenderStringService,
  ) {
  }

  title() {
    return (this.customDisplay as IHttpPortDisplay)?.title;
  }

  url() {
    return this.renderStringService.renderString(
      (this.customDisplay as IHttpPortDisplay)?.url,
      this.containerPorts ? this.containerPorts : [],
      this.username ? this.username : '',
      this.password ? this.password : '',
      this.ipAddress ? this.ipAddress : '',
      this.userRedirect ? this.userRedirect : ''
    )
  }
}
