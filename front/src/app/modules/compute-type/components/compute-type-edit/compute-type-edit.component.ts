import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IComputeType } from '../../interfaces/compute-type';
import { FormBuilder, FormControl } from '@angular/forms';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { ComputeTypesApiService } from '../../services/compute-types-api.service';
import { IOvhResource } from '../../../sessions-resources/interfaces/ovh-resource';

@Component({
  selector: 'app-compute-type-edit',
  templateUrl: './compute-type-edit.component.html',
  styleUrls: ['./compute-type-edit.component.css']
})
export class ComputeTypeEditComponent implements OnInit {

  regions: string[] = [];
  ovhResources: IOvhResource[] = [];
  computeTypeForm: FormControl;
  computeType?: IComputeType;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly computeTypeApi: ComputeTypesApiService,
    private readonly activatedRoute: ActivatedRoute
  ) {
    this.computeTypeForm = fb.control({});
    this.route.data.subscribe(({computeType}) => {
      this.computeTypeForm.setValue({
        ...computeType,
        autoscalingResource: computeType?.autoscalingResource?.id || null,
      });
      this.computeType = computeType;
    })
  }


  ngOnInit(): void {
    this.activatedRoute.data.subscribe(
      data => {
        this.regions = data.regions;
        this.ovhResources = data.resources;
      }
    )
  }


  submit() {
    if (this.computeType) {
      this.toasterService.toast(
        this.computeTypeApi.editComputeType(this.computeType.id, this.computeTypeForm.value),
        "Le type de charge a bien été mis à jour",
        "Erreur lors de la sauvegarde du type de charge",
        false,
        ['/admin/compute-types']
      )
    }
  }

}
