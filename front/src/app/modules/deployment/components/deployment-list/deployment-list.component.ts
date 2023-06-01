import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from "@angular/router";
import { IDeployment } from "../../interfaces/deployment";
import { ISession } from "../../../shell/interfaces/session";
import { DeploymentApiService } from "../../services/deployment-api.service";
import { ConfirmDialogService } from "../../../utils/confirm-dialog/confirm-dialog.service";
import { formatDate } from "@angular/common";
import { mergeMap } from "rxjs/operators";
import { filter } from "rxjs/operators";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';

@Component({
  selector: 'app-deployment-list',
  templateUrl: './deployment-list.component.html',
  styleUrls: ['./deployment-list.component.css']
})
export class DeploymentListComponent implements OnInit {
  deployments: IDeployment[] = [];
  columnsToDisplay = ['type', 'startDateTime', 'courses', 'resources', 'description', 'action'];
  resources: IOvhResource[] = [];
  computeTypes: IComputeType[] = [];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly deploymentApiService: DeploymentApiService,
    private readonly dialogConfirmService: ConfirmDialogService,
  ) { }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(
      data => {
        this.deployments = data.deployments;
        this.resources = data.resources;
        this.computeTypes = data.computeTypes;
      }
    );
  }

  displayCourse(sessions: ISession[]): string {
    return sessions.reduce((acc, s, index) =>
      acc + (index === 0 ? '' : ', ') + s.course.title, ''
    );
  }

  delete(deployment: IDeployment) {
    this.dialogConfirmService.confirm({
      text: 'Confirmez-vous la suppression du déploiement du ' + formatDate(deployment.startDateTime, 'd MMMM y à hh:mm', 'fr') + ' ?'
    }).pipe(
      filter(confirm => confirm),
      mergeMap(() => this.deploymentApiService.deleteDeployment(deployment.id)),
      mergeMap(() => this.deploymentApiService.getDeployments())
    ).subscribe(
      deployments => this.deployments = deployments
    );
  }

  findResource(resource: number): string | undefined {
    return this.resources.find(r => r.id === resource)?.type;
  }

  findComputeType(computeTypeId: number): string | undefined {
    return this.computeTypes.find(r => r.id === computeTypeId)?.displayName;
  }
}
