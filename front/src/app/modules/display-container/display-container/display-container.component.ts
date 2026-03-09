import { Component, EventEmitter, Input, OnInit, Output } from "@angular/core";
import { ContainerSwarmState, ContainerSwarmStateMessages, IContainer } from "../../shell/interfaces/container";
import { ClipboardSnackService } from "../../utils/snack-notification/clipboard-snack.service";
import { ICourseDisplay, IHttpPortDisplay, IPortDisplay } from "../../admin-course/interfaces/course-display";
import { IContainerPort } from "../../shell/interfaces/container-port";


@Component({
  selector: 'app-display-container',
  templateUrl: './display-container.component.html',
  styleUrls: ['./display-container.component.css'],
})
export class DisplayContainerComponent implements OnInit {
  @Input() container: IContainer | null = null;
  @Input() displayOptions: ICourseDisplay | undefined = undefined;
  @Input() userRedirect: string | undefined = 'USER_REDIRECT';
  @Input() enableAutoClick: boolean = false;
  @Output() deleteEnv = new EventEmitter<void>();

  autoClick: boolean = false;

  private readonly USER_REDIRECT_REGEX = /USER[_-]REDIRECT/;

  constructor(
    private readonly clipboardSnackService: ClipboardSnackService,
  ) {
  }

  ngOnInit(): void {
    this.autoClick = this.enableAutoClick && this.shouldAutoclick()
  }

  shouldAutoclick(): boolean {
    const customPorts = this.displayOptions?.customPortsDisplay ?? [];

    if (customPorts.length > 1) {
      return false;
    }

    const displayedPortsCount = this.countDisplayedPorts(customPorts.length);

    if (displayedPortsCount > 1) {
      return false;
    }

    const port = customPorts.length === 1
      ? (customPorts[0] as IHttpPortDisplay)
      : undefined;

    return this.canAutoclick(port);
  }

  private countDisplayedPorts(initialCount: number): number {
    if (!this.container) {
      return initialCount;
    }

    let count = initialCount;

    for (const port of this.container.ports) {
      if (this.shouldDisplay(port)) {
        count++;
        if (count > 1) {
          return count;
        }
      }
    }

    return count;
  }

  private canAutoclick(port?: IHttpPortDisplay): boolean {
    if (!this.displayOptions) {
      return false;
    }

    if (!this.displayOptions.displayPassword && !this.displayOptions.displayUsername) {
      return true;
    }

    if (port?.url?.includes('{{PASSWORD}}')) {
      return true;
    }

    if (
      this.userRedirect &&
      this.userRedirect !== 'USER_REDIRECT' &&
      port &&
      !this.portDisplayHasNoUserRedirect(port)
    ) {
      return true;
    }

    return false;
  }

  copyText(text: string | undefined) {
    if (text) {
      this.clipboardSnackService.copyWithNotification(text);
    }
  }

  showCustomDisplay(customDisplay: IPortDisplay): boolean {
    if (undefined !== this.userRedirect
      && 'USER_REDIRECT' !== this.userRedirect
      && this.portDisplayHasNoUserRedirect(customDisplay as IHttpPortDisplay)) {
        return false;
    }
    return true;
  }

  portDisplayHasNoUserRedirect(customDisplay: IHttpPortDisplay): boolean {
    return !this.USER_REDIRECT_REGEX.test(customDisplay.url);
  }

  shouldDisplay(containerPort: IContainerPort): boolean {
    // console.log(containerPort)
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
