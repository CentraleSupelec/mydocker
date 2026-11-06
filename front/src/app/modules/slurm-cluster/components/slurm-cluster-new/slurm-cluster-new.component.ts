import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl } from '@angular/forms';
import { ObservableSnackNotificationService } from '../../../utils/snack-notification/observable-snack-notification.service';
import { SlurmClustersApiService } from '../../services/slurm-clusters-api.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-slurm-cluster-new',
  templateUrl: './slurm-cluster-new.component.html',
  styleUrls: ['./slurm-cluster-new.component.css'],
})
export class SlurmClusterNewComponent implements OnInit {
  slurmClusterForm: FormControl;

  constructor(
    private readonly fb: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly slurmClusterApi: SlurmClustersApiService,
    private readonly translate: TranslateService
  ) {
    this.slurmClusterForm = fb.control({});
  }

  ngOnInit(): void {
    null;
  }

  submit() {
    this.toasterService.toast(
      this.slurmClusterApi.newSlurmCluster(this.slurmClusterForm.value),
      this.translate.instant('admin.resources_management.slurm_clusters.create_success'),
      this.translate.instant('admin.resources_management.slurm_clusters.create_error'),
      false,
      ['/admin/slurm-clusters']
    );
  }
}
