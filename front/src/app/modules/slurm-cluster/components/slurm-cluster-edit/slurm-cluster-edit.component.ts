import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ISlurmCluster } from '../../interfaces/slurm-cluster';
import { FormBuilder, FormControl } from '@angular/forms';
import { ObservableSnackNotificationService } from '../../../utils/snack-notification/observable-snack-notification.service';
import { SlurmClustersApiService } from '../../services/slurm-clusters-api.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-slurm-cluster-edit',
  templateUrl: './slurm-cluster-edit.component.html',
  styleUrls: ['./slurm-cluster-edit.component.css'],
})
export class SlurmClusterEditComponent implements OnInit {
  slurmClusterForm: FormControl;
  slurmCluster?: ISlurmCluster;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly slurmClusterApi: SlurmClustersApiService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly translate: TranslateService
  ) {
    this.slurmClusterForm = fb.control({});
    this.route.data.subscribe(({ slurmCluster }) => {
      this.slurmClusterForm.setValue(slurmCluster);
      this.slurmCluster = slurmCluster;
    });
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe((data) => {});
  }

  submit() {
    if (this.slurmCluster) {
      this.toasterService.toast(
        this.slurmClusterApi.editSlurmCluster(
          this.slurmCluster.id,
          this.slurmClusterForm.value
        ),
        this.translate.instant('admin.resources_management.slurm_clusters.edit.success'),
        this.translate.instant('admin.resources_management.slurm_clusters.edit.error'),
        false,
        ['/admin/slurm-clusters']
      );
    }
  }
}
