import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from "@angular/router";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";
import { FormBuilder, FormControl } from "@angular/forms";
import { DeploymentApiService } from "../../services/deployment-api.service";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-deployment-create',
  templateUrl: './deployment-create.component.html',
  styleUrls: ['./deployment-create.component.css']
})
export class DeploymentCreateComponent implements OnInit {
  regions: string[] = [];
  ovhResources: IOvhResource[] = [];
  readonly deployFormControl: FormControl;
  computeTypes: IComputeType[] = [];

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly deploymentApiService: DeploymentApiService,
    private readonly toasterService: ObservableSnackNotificationService,
    formBuilder: FormBuilder,
    private readonly translate: TranslateService
  ) {
    this.deployFormControl = formBuilder.control({});
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(
      data => {
        this.regions = data.regions;
        this.ovhResources = data.resources;
        this.computeTypes = data.computeTypes;
      }
    )
  }

  submit(): void {
    this.toasterService.toast(
      this.deploymentApiService.createDeployment(this.deployFormControl.value),
      this.translate.instant('admin.resources_management.deployment.create_success'),
      this.translate.instant('admin.resources_management.deployment.create_error'),
      false,
      ['/admin/deployment']
    )
  }
}
