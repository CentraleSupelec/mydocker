import { Component, Input } from '@angular/core';
import { BuildStatus } from "../../interfaces/build-status";

@Component({
  selector: 'app-docker-image-build-status',
  templateUrl: './docker-image-build-status.component.html',
  styleUrls: ['./docker-image-build-status.component.css']
})
export class DockerImageBuildStatusComponent {
  @Input() status: BuildStatus | null = null;
  @Input() successToolTip: string = 'Build terminé avec succès';
  @Input() errorToolTip: string = 'Build en erreur';
  @Input() buildingToolTip: string = 'Build en cours';

  constructor() { }
}
