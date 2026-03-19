import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ContentAccessRoutingModule } from './content-access-routing.module';
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { RouterModule } from "@angular/router";
import { ContentShowComponent } from './components/content-show/content-show.component';
import { MatExpansionModule } from "@angular/material/expansion";
import { FlexLayoutModule } from "@angular/flex-layout";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { ClipboardModule } from "@angular/cdk/clipboard";
import { NgxPermissionsModule } from "ngx-permissions";
import { DisplayContainerModule } from "../display-container/display-container.module";
import { ConfirmDialogModule } from "../utils/confirm-dialog/confirm-dialog.module";
import { APP_CONFIG, appConstantFactory } from "../../app-config";
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { MatDividerModule } from "@angular/material/divider";
import { MatTabsModule } from "@angular/material/tabs";
import { MatCardModule } from "@angular/material/card";
import { MatTooltipModule } from "@angular/material/tooltip";
import { LogDialogModule } from "../log-dialog/log-dialog.module";
import { MatProgressBarModule } from "@angular/material/progress-bar";


@NgModule({
  declarations: [
    ContentShowComponent,
  ],
    imports: [
        CommonModule,
        ContentAccessRoutingModule,
        MatToolbarModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatExpansionModule,
        RouterModule,
        FlexLayoutModule,
        MatFormFieldModule,
        MatInputModule,
        ClipboardModule,
        NgxPermissionsModule.forChild(),
        DisplayContainerModule,
        ConfirmDialogModule,
        MatSnackBarModule,
        MatDividerModule,
        MatTabsModule,
        MatCardModule,
        MatTooltipModule,
        LogDialogModule,
        MatProgressBarModule,
    ],
  providers: [
    {
      provide: APP_CONFIG,
      useFactory: appConstantFactory
    },
  ],
})
export class ContentAccessModule { }
