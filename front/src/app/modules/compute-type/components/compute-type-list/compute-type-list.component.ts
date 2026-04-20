import { Component, Inject, OnInit } from "@angular/core";
import { ActivatedRoute } from '@angular/router';
import { IComputeType, IOvhRegion } from '../../interfaces/compute-type';
import { ConfirmDialogService } from '../../../utils/confirm-dialog/confirm-dialog.service';
import { ComputeTypesApiService } from '../../services/compute-types-api.service';
import { filter, map, mergeMap } from 'rxjs/operators';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import { TranslateService } from "@ngx-translate/core";

@Component({
  selector: 'app-compute-type-list',
  templateUrl: './compute-type-list.component.html',
  styleUrls: ['./compute-type-list.component.css']
})
export class ComputeTypeListComponent implements OnInit {

  computeTypes: IComputeType[] = [];
  columnsToDisplay = ['id', 'displayName', 'technicalName', 'gpu', 'storageBackend']

  constructor(
    private readonly route: ActivatedRoute,
    private readonly confirmDialogService: ConfirmDialogService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly computeTypesApi: ComputeTypesApiService,
    private translate: TranslateService,
    @Inject(APP_CONFIG) readonly config: IAppConfig
  ) {
  }

  ngOnInit(): void {
    this.route.data.subscribe(({computeTypes}) => {
      this.computeTypes = computeTypes;
    });
    if (this.config.deployment_enabled) {
      this.columnsToDisplay.push('autoscaling', 'action');
    } else {
      this.columnsToDisplay.push('action');
    }
  }

  remove(computeType: IComputeType) {
    this.translate
      .get([
        'admin.resources_management.compute_types.actions.delete_confirmation',
        'admin.resources_management.compute_types.actions.delete_success',
        'admin.resources_management.compute_types.actions.delete_error'
      ], { name: computeType.displayName })
      .subscribe(translations => {
        this.toasterService.toast(
          this.confirmDialogService
            .confirm({
              text: translations['admin.resources_management.compute_types.actions.delete_confirmation']
            })
            .pipe(
              filter((confirm: boolean) => confirm),
              mergeMap(() => this.computeTypesApi.deleteComputeType(computeType.id)),
              mergeMap(() => this.computeTypesApi.getComputeTypes()),
              map((computeTypes) => {
                this.computeTypes = computeTypes;
              })
            ),
          translations['admin.resources_management.compute_types.actions.delete_success'],
          translations['admin.resources_management.compute_types.actions.delete_error']
        );
      });
  }

  mapRegions(regions: Array<IOvhRegion>): Array<String> {
    return regions.map(region => region.region);
  }
}
