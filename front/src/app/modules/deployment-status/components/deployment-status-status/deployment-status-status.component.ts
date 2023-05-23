import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-deployment-status-status',
  templateUrl: 'deployment-status-status.component.html',
  styleUrls: ['deployment-status-status.component.css']
})
export class DeploymentStatusStatusComponent {
  @Input() status: 'DONE' | 'ERROR' | 'RUNNING' | 'CREATED' | 'SKIPPED' | null = null;

  constructor() { }
}
