import { Component, OnInit } from '@angular/core';
import { IDeployment } from "../../interfaces/deployment";
import { ActivatedRoute } from "@angular/router";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';

@Component({
  selector: 'app-deployment-view',
  templateUrl: './deployment-view.component.html',
  styleUrls: ['./deployment-view.component.css']
})
export class DeploymentViewComponent implements OnInit {
  deployment: IDeployment | undefined;
  resources: IOvhResource[] = [];
  computeTypes: IComputeType[] = [];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
  ) { }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(
      d => {
        this.deployment = d.deployment;
        this.resources = d.resources;
        this.computeTypes = d.computeTypes;
      }
    );
  }

  findResource(resource: number): string | undefined {
    return this.resources.find(r => r.id === resource)?.type;
  }

  findComputeType(computeTypeId: number): string | undefined {
    return this.computeTypes.find(r => r.id === computeTypeId)?.displayName;
  }
}
