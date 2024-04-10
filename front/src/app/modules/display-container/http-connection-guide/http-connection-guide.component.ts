import { Component, Input } from '@angular/core';
import { IContainerPort } from "../../shell/interfaces/container-port";

@Component({
  selector: 'app-http-connexion-guide',
  templateUrl: './http-connection-guide.component.html',
  styleUrls: ['./http-connection-guide.component.css']
})
export class HttpConnectionGuideComponent {
  @Input() containerPort: IContainerPort | null = null;
  @Input() ipAddress: string | undefined = '';

  getContainerUrl(): string {
    return this.containerPort?.hostname ? "https://" + this.containerPort?.hostname: "http://" + this.ipAddress + ':' + this.containerPort?.portMapTo
  }
}
