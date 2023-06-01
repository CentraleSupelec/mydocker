import { Component, Input } from '@angular/core';
import { IContainer } from "../../shell/interfaces/container";
import { ClipboardSnackService } from "../../utils/snack-notification/clipboard-snack.service";
import { ICourseDisplay } from "../../admin-course/interfaces/course-display";
import { IContainerPort } from "../../shell/interfaces/container-port";


@Component({
  selector: 'app-display-container',
  templateUrl: './display-container.component.html',
  styleUrls: ['./display-container.component.css'],
})
export class DisplayContainerComponent {
  @Input() container: IContainer | null = null;
  @Input() displayOptions: ICourseDisplay | undefined = undefined;

  constructor(
    private readonly clipboardSnackService: ClipboardSnackService,
  ) {
  }

  copyText(text: string | undefined) {
    if (text) {
      this.clipboardSnackService.copyWithNotification(text);
    }
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
}
