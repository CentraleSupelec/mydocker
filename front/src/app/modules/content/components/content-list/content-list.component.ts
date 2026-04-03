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
    @Inject(APP_CONFIG) readonly config: IAppConfig
  ) {
  }


  ngOnInit(): void {
    this.route.data.subscribe(({contents}) => {
      this.contents = contents;
    });
  }

  remove(content: IContent) {
    this.toasterService
      .toast(
        this.confirmDialogService
          .confirm({
            text: `Souhaitez-vous vraiment supprimer ce contenu "${content.title}" ?`
          })
          .pipe(
            filter((confirm: boolean) => confirm),
            mergeMap(() => this.contentsApi.deleteContent(content.id)),
            mergeMap(() => this.contentsApi.getContents()),
            map((contents) => {
              this.contents = contents;
            })
          ),
        `Le contenu "${content.title}" a bien été supprimé`,
        `Impossible de supprimer le contenu "${content.title}"`
      );
  }
}
