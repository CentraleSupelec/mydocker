import { Component, Input } from '@angular/core';
import { ClipboardSnackService } from "../../utils/snack-notification/clipboard-snack.service";
import { IContainerPort } from "../../shell/interfaces/container-port";

@Component({
  selector: 'app-ssh-connexion-guide',
  templateUrl: './ssh-connection-guide.component.html',
  styleUrls: ['./ssh-connection-guide.component.css']
})
export class SshConnectionGuideComponent {
  @Input() containerPort: IContainerPort | null = null;
  @Input() ipAddress: string | undefined = '';
  @Input() username: string | undefined = '';

  constructor(
    private readonly clipboardSnackService: ClipboardSnackService
  ) {
  }


  copySshCommand() {
    this.clipboardSnackService.copyWithNotification(
      `ssh ${this.username }@${ this.ipAddress } -p ${ this.containerPort?.portMapTo }`
    );
  }
}
