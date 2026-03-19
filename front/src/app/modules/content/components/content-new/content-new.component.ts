import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl } from '@angular/forms';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { ContentsApiService } from '../../services/contents-api.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-content-new',
  templateUrl: './content-new.component.html',
  styleUrls: ['./content-new.component.css']
})
export class ContentNewComponent {
  contentForm: FormControl;

  constructor(
    private readonly fb: FormBuilder,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly contentApi: ContentsApiService,
    private readonly activatedRoute: ActivatedRoute,
  ) {
    this.contentForm = fb.control({});
  }

  submit() {
    this.toasterService.toast(
      this.contentApi.newContent(this.contentForm.value),
      "Le contenu a bien été créé",
      "Erreur lors de la création du contenu",
      false,
      ['/admin/contents']
    );
  }
}
