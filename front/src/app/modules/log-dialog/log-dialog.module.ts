import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LogDialogComponent } from "./log-dialog/log-dialog.component";
import { MatDialogModule } from "@angular/material/dialog";
import { FlexModule } from "@angular/flex-layout";
import { MatButtonModule } from "@angular/material/button";
import { OpenLogDialogService } from "./open-log-dialog.service";
import { TranslateModule } from '@ngx-translate/core';



@NgModule({
  declarations: [
    LogDialogComponent
  ],
  imports: [
    CommonModule,
    MatDialogModule,
    FlexModule,
    MatButtonModule,
    TranslateModule
  ],
  providers: [
    OpenLogDialogService
  ]
})
export class LogDialogModule { }
