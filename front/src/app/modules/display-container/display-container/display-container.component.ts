import { Component, EventEmitter, Input, Output } from "@angular/core";
import { ContainerSwarmState, ContainerSwarmStateMessages, IContainer } from "../../shell/interfaces/container";
import { ClipboardSnackService } from "../../utils/snack-notification/clipboard-snack.service";
import { ICourseDisplay, IHttpPortDisplay, IPortDisplay } from "../../admin-course/interfaces/course-display";
import { IContainerPort } from "../../shell/interfaces/container-port";


@Component({
  selector: 'app-display-container',
  templateUrl: './display-container.component.html',
  styleUrls: ['./display-container.component.css'],
})
export class DisplayContainerComponent {
  @Input() container: IContainer | null = null;
  @Input() displayOptions: ICourseDisplay | undefined = undefined;
  @Input() userRedirect: string | undefined = 'USER_REDIRECT';
  @Output() deleteEnv = new EventEmitter<void>();

  constructor(
    private readonly clipboardSnackService: ClipboardSnackService,
  ) {
  }

  copyText(text: string | undefined) {
    if (text) {
      this.clipboardSnackService.copyWithNotification(text);
    }
  }

  showCustomDisplay(customDisplay: IPortDisplay): boolean {
    if (undefined !== this.userRedirect 
      && 'USER_REDIRECT' !== this.userRedirect
      && -1 == (customDisplay as IHttpPortDisplay).url.search('USER_REDIRECT')
      && -1 == (customDisplay as IHttpPortDisplay).url.search('USER-REDIRECT')) {
        return false;
    }
    return true;
  }

  shouldDisplay(containerPort: IContainerPort): boolean {
    if (this.displayOptions === undefined || this.displayOptions.displayPorts === undefined) {
      return true
    }

    const key = String(containerPort.mapPort)
    if (key in this.displayOptions.displayPorts) {
      return this.displayOptions.displayPorts[key];
    }
    return true;
  }

  displayUsername(): boolean {
    if (this.displayOptions === undefined || this.displayOptions.displayUsername === undefined) {
      return true
    }
    return this.displayOptions.displayUsername;
  }

  displayPassword(): boolean {
    if (this.displayOptions === undefined || this.displayOptions.displayPassword === undefined) {
      return true
    }
    return this.displayOptions.displayPassword;
  }

  onDeleteEnv(): void {
    this.deleteEnv.emit();
  }

  public readonly ContainerSwarmState = ContainerSwarmState;
  public readonly ContainerSwarmStateMessages = ContainerSwarmStateMessages;
}
