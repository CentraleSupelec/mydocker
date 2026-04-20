import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl } from '@angular/forms';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { ComputeTypesApiService } from '../../services/compute-types-api.service';
import { ActivatedRoute } from '@angular/router';
import { IOvhResource } from '../../../sessions-resources/interfaces/ovh-resource';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-compute-type-new',
  templateUrl: './compute-type-new.component.html',
  styleUrls: ['./compute-type-new.component.css']
})
export class ComputeTypeNewComponent implements OnInit {
  regions: string[] = [];
  ovhResources: IOvhResource[] = [];
  computeTypeForm: FormControl;

  constructor(
    private readonly fb: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly computeTypeApi: ComputeTypesApiService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly translate: TranslateService
  ) {
    this.computeTypeForm = fb.control({});
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
    this.toasterService.toast(
      this.computeTypeApi.newComputeType(this.computeTypeForm.value),
      this.translate.instant('admin.resources_management.compute_types.create_success'),
      this.translate.instant('admin.resources_management.compute_types.create_error'),
      false,
      ['/admin/compute-types']
    );
  }
}
