import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IComputeType, IOvhRegion } from '../../interfaces/compute-type';
import { ConfirmDialogService } from '../../../utils/confirm-dialog/confirm-dialog.service';
import { ComputeTypesApiService } from '../../services/compute-types-api.service';
import { filter, map, mergeMap } from 'rxjs/operators';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';

@Component({
  selector: 'app-compute-type-list',
  templateUrl: './compute-type-list.component.html',
  styleUrls: ['./compute-type-list.component.css']
})
export class ComputeTypeListComponent implements OnInit {

  computeTypes: IComputeType[] = [];
  columnsToDisplay = ['id', 'displayName', 'technicalName', 'gpu', 'autoscaling', 'action']

  constructor(
    private readonly route: ActivatedRoute,
    private readonly confirmDialogService: ConfirmDialogService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly computeTypesApi: ComputeTypesApiService
  ) {
  }

  ngOnInit(): void {
    this.route.data.subscribe(({computeTypes}) => {
      this.computeTypes = computeTypes;
    });
  }

  remove(computeType: IComputeType) {
    this.toasterService
      .toast(
        this.confirmDialogService
          .confirm({
            text: `Souhaitez-vous vraiment supprimer le type de charge "${computeType.displayName}" ?`
          })
          .pipe(
            filter((confirm: boolean) => confirm),
            mergeMap(() => this.computeTypesApi.deleteComputeType(computeType.id)),
            mergeMap(() => this.computeTypesApi.getComputeTypes()),
            map((computeTypes) => {
              this.computeTypes = computeTypes;
            })
          ),
        `Le type de charge "${computeType.displayName}" a bien été supprimé`,
        `Impossible de supprimer le type de charge "${computeType.displayName}"`
      );
  }

  mapRegions(regions: Array<IOvhRegion>): Array<String> {
    return regions.map(region => region.region);
  }
}
