import { Component } from '@angular/core';
import { FormBuilder, FormControl } from '@angular/forms';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { ClustersApiService } from '../../services/clusters-api.service';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-cluster-new',
  templateUrl: './cluster-new.component.html',
  styleUrls: ['./cluster-new.component.css']
})
export class ClusterNewComponent {
  clusterForm: FormControl;

  constructor(
    private readonly fb: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly clusterApi: ClustersApiService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly translate: TranslateService
  ) {
    this.clusterForm = fb.control({});
  }

  submit() {
    this.toasterService.toast(
      this.clusterApi.newCluster(this.clusterForm.value),
      this.translate.instant('admin.resources_management.compute_types.create_success'),
      this.translate.instant('admin.resources_management.compute_types.create_error'),
      false,
      ['/admin/clusters']
    );
  }
}
