import { Component, Inject, OnInit } from "@angular/core";
import { ActivatedRoute } from '@angular/router';
import { IContent } from '../../interfaces/content';
import { ConfirmDialogService } from '../../../utils/confirm-dialog/confirm-dialog.service';
import { ContentsApiService } from '../../services/contents-api.service';
import { filter, map, mergeMap } from 'rxjs/operators';
import {
  ObservableSnackNotificationService
} from '../../../utils/snack-notification/observable-snack-notification.service';
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import { TranslateService } from "@ngx-translate/core";

@Component({
  selector: 'app-content-list',
  templateUrl: './content-list.component.html',
  styleUrls: ['./content-list.component.css']
})
export class ContentListComponent {

  contents: IContent[] = [];
  columnsToDisplay = ['id', 'title', 'slug', 'enabled', 'action']

  constructor(
    private readonly route: ActivatedRoute,
    private readonly confirmDialogService: ConfirmDialogService,
    private readonly toasterService: ObservableSnackNotificationService,
    private readonly contentsApi: ContentsApiService,
    private translate: TranslateService,
    @Inject(APP_CONFIG) readonly config: IAppConfig
  ) {
  }


  ngOnInit(): void {
    this.route.data.subscribe(({contents}) => {
      this.contents = contents;
    });
  }

  remove(content: IContent) {

    this.translate
      .get([
        'admin.resources_management.content_management.actions.delete_confirmation',
        'admin.resources_management.content_management.actions.delete_success',
        'admin.resources_management.content_management.actions.delete_error'
      ], { name: content.title })
      .subscribe(translations => {
        this.toasterService
        .toast(
          this.confirmDialogService
            .confirm({
              text: translations['admin.resources_management.content_management.actions.delete_confirmation']
            })
            .pipe(
              filter((confirm: boolean) => confirm),
              mergeMap(() => this.contentsApi.deleteContent(content.id)),
              mergeMap(() => this.contentsApi.getContents()),
              map((contents) => {
                this.contents = contents;
              })
            ),
            translations['admin.resources_management.content_management.actions.delete_success'],
            translations['admin.resources_management.content_management.actions.delete_error']
        );
      })
  }
}
