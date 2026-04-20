import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IContent } from '../../interfaces/content';
import { FormBuilder, FormControl } from '@angular/forms';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { ContentsApiService } from '../../services/contents-api.service';
import { IOvhResource } from '../../../sessions-resources/interfaces/ovh-resource';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-content-edit',
  templateUrl: './content-edit.component.html',
  styleUrls: ['./content-edit.component.css']
})
export class ContentEditComponent {
  contentForm: FormControl;
  content?: IContent;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly contentApi: ContentsApiService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly translate: TranslateService
  ) {
    this.contentForm = fb.control({});
    this.route.data.subscribe(({content}) => {
      this.contentForm.setValue({
        ...content,
        autoscalingResource: content?.autoscalingResource?.id || null,
      });
      this.content = content;
    })
  }

  submit() {
    if (this.content) {
      this.toasterService.toast(
        this.contentApi.editContent(this.content.id, this.contentForm.value),
        this.translate.instant('admin.resources_management.content_management.edit.success'),
        this.translate.instant('admin.resources_management.content_management.edit.error'),
        false,
        ['/admin/contents']
      )
    }
  }

}
