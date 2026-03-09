import { Component, Input } from '@angular/core';
import { ClipboardSnackService } from "../../utils/snack-notification/clipboard-snack.service";
import { IContainerPort } from "../../shell/interfaces/container-port";

@Component({
  selector: 'app-tcp-connexion-guide',
  templateUrl: './tcp-connection-guide.component.html',
  styleUrls: ['./tcp-connection-guide.component.css']
})
export class TcpConnectionGuideComponent {
  @Input() containerPort: IContainerPort | null = null;
  @Input() ipAddress: string | undefined = '';
  @Input() username: string | undefined = '';

  constructor(
    private readonly clipboardSnackService: ClipboardSnackService
  ) {
  }

  copyIpAddress() {
    this.clipboardSnackService.copyWithNotification(this.ipAddress?? "");
  }

  copyPort() {
    this.clipboardSnackService.copyWithNotification(`${this.containerPort?.portMapTo}`);
  }
}
