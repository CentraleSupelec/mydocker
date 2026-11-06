import { Component, Inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ISlurmCluster } from '../../interfaces/slurm-cluster';
import { ConfirmDialogService } from '../../../utils/confirm-dialog/confirm-dialog.service';
import { SlurmClustersApiService } from '../../services/slurm-clusters-api.service';
import { filter, map, mergeMap } from 'rxjs/operators';
import { ObservableSnackNotificationService } from '../../../utils/snack-notification/observable-snack-notification.service';
import { APP_CONFIG, IAppConfig } from '../../../../app-config';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-slurm-cluster-list',
  templateUrl: './slurm-cluster-list.component.html',
  styleUrls: ['./slurm-cluster-list.component.css'],
})
export class SlurmClusterListComponent implements OnInit {
  slurmClusters: ISlurmCluster[] = [];
  // Admin list shows id + displayName + technicalName + apiEndpoint, fed by the
  // ADMIN-only /details endpoint. The secret is never returned or rendered
  columnsToDisplay = [
    'id',
    'displayName',
    'technicalName',
    'apiEndpoint',
    'action',
  ];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly confirmDialogService: ConfirmDialogService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly slurmClustersApi: SlurmClustersApiService,
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe(({ slurmClusters }) => {
      this.slurmClusters = slurmClusters;
    });
  }

  remove(slurmCluster: ISlurmCluster) {
    this.toasterService.toast(
      this.confirmDialogService
        .confirm({
          text: this.translate.instant(
            'admin.resources_management.slurm_clusters.actions.remove_confirmation',
            { name: slurmCluster.displayName }
          ),
        })
        .pipe(
          filter((confirm: boolean) => confirm),
          mergeMap(() =>
            this.slurmClustersApi.deleteSlurmCluster(slurmCluster.id)
          ),
          mergeMap(() => this.slurmClustersApi.getSlurmClusterDetails()),
          map((slurmClusters) => {
            this.slurmClusters = slurmClusters;
          })
        ),
      this.translate.instant(
        'admin.resources_management.slurm_clusters.actions.remove_success',
        { name: slurmCluster.displayName }
      ),
      this.translate.instant(
        'admin.resources_management.slurm_clusters.actions.remove_error',
        { name: slurmCluster.displayName }
      )
    );
  }
}
