import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ShellRoutingModule } from './shell-routing.module';
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { RouterModule } from "@angular/router";
import { CourseJoinComponent } from "./components/course-join/course-join.component";
import { CourseListComponent } from './components/course-list/course-list.component';
import { MatExpansionModule } from "@angular/material/expansion";
import { FlexLayoutModule } from "@angular/flex-layout";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { ClipboardModule } from "@angular/cdk/clipboard";
import { ConnexionHelperComponent } from './components/connexion-helper/connexion-helper.component';
import { NgxPermissionsModule } from "ngx-permissions";
import { ShellAccessComponent } from "./components/shell-access/shell-access.component";
import { DisplayContainerModule } from "../display-container/display-container.module";
import { ConfirmDialogModule } from "../utils/confirm-dialog/confirm-dialog.module";
import { APP_CONFIG, appConstantFactory } from "../../app-config";
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { SaveStateComponent } from './components/save-state/save-state.component';
import { CourseDetailComponent } from './components/course-detail/course-detail.component';
import { MatDividerModule } from "@angular/material/divider";
import { MatTabsModule } from "@angular/material/tabs";
import { MatTooltipModule } from "@angular/material/tooltip";
import { LogDialogModule } from "../log-dialog/log-dialog.module";
import { CountdownComponent } from './components/countdown/countdown.component';


@NgModule({
  declarations: [
    CourseJoinComponent,
    CourseListComponent,
    ConnexionHelperComponent,
    ShellAccessComponent,
    SaveStateComponent,
    CourseDetailComponent,
    CountdownComponent,
  ],
    imports: [
        CommonModule,
        ShellRoutingModule,
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
        MatTooltipModule,
        LogDialogModule,
    ],
  providers: [
    {
      provide: APP_CONFIG,
      useFactory: appConstantFactory
    },
  ],
})
export class ShellModule { }
