import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ICluster } from '../../interfaces/cluster';
import { FormBuilder, FormControl } from '@angular/forms';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { ClustersApiService } from '../../services/clusters-api.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-cluster-edit',
  templateUrl: './cluster-edit.component.html',
  styleUrls: ['./cluster-edit.component.css']
})
export class ClusterEditComponent {

  clusterForm: FormControl;
  cluster?: ICluster;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly clusterApi: ClustersApiService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly translate: TranslateService
  ) {
    this.clusterForm = fb.control({});
    this.route.data.subscribe(({cluster}) => {
      this.clusterForm.setValue({
        ...cluster,
      });
      this.cluster = cluster;
    })
  }

  submit() {
    if (this.cluster) {
      this.toasterService.toast(
        this.clusterApi.editCluster(this.cluster.id, this.clusterForm.value),
        this.translate.instant('admin.resources_management.clusters.edit.success'),
        this.translate.instant('admin.resources_management.clusters.edit.error'),
        false,
        ['/admin/clusters']
      )
    }
  }

}
