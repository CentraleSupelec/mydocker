import { Component, Inject, OnInit } from "@angular/core";
import { ActivatedRoute } from '@angular/router';
import { ICluster } from '../../interfaces/cluster';
import { ConfirmDialogService } from '../../../utils/confirm-dialog/confirm-dialog.service';
import { ClustersApiService } from '../../services/clusters-api.service';
import { filter, map, mergeMap } from 'rxjs/operators';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import { TranslateService } from "@ngx-translate/core";

@Component({
  selector: 'app-cluster-list',
  templateUrl: './cluster-list.component.html',
  styleUrls: ['./cluster-list.component.css']
})
export class ClusterListComponent implements OnInit {

  clusters: ICluster[] = [];
  columnsToDisplay = ['id', 'name', 'goAddress', 'goPort', "deploymentEnabled", "action"]

  constructor(
    private readonly route: ActivatedRoute,
    private readonly confirmDialogService: ConfirmDialogService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly clustersApi: ClustersApiService,
    private readonly translate: TranslateService,
    @Inject(APP_CONFIG) readonly config: IAppConfig
  ) {
  }

  ngOnInit(): void {
    this.route.data.subscribe(({clusters}) => {
      this.clusters = clusters;
    });
  }

  remove(cluster: ICluster) {
    this.toasterService
      .toast(
        this.confirmDialogService
          .confirm({
            text: this.translate.instant('admin.resources_management.clusters.actions.delete_confirmation', { name: cluster.name})
          })
          .pipe(
            filter((confirm: boolean) => confirm),
            mergeMap(() => this.clustersApi.deleteCluster(cluster.id)),
            mergeMap(() => this.clustersApi.getClusters()),
            map((clusters) => {
              this.clusters = clusters;
            })
          ),
          this.translate.instant('admin.resources_management.clusters.actions.delete_success', { name: cluster.name}),
          this.translate.instant('admin.resources_management.clusters.actions.delete_error', { name: cluster.name})
      );
  }
}
