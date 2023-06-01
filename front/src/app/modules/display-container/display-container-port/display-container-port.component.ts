import { Component, Input } from '@angular/core';
import { IContainerPort } from "../../shell/interfaces/container-port";

@Component({
  selector: 'app-display-container-port',
  templateUrl: './display-container-port.component.html',
  styleUrls: ['./display-container-port.component.css']
})
export class DisplayContainerPortComponent {
  @Input() containerPort: IContainerPort | null = null;
  @Input() username: string | undefined = '';
  @Input() ipAddress: string | undefined = '';
  @Input() shouldDisplay = true;
}
