import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SnackNotificationComponent } from "./snack-notification/snack-notification.component";
import { FlexLayoutModule } from "@angular/flex-layout";
import { MatIconModule } from "@angular/material/icon";
import { MatSnackBarModule } from "@angular/material/snack-bar";



@NgModule({
  declarations: [
    SnackNotificationComponent,
  ],
  imports: [
    CommonModule,
    FlexLayoutModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  entryComponents: [
    SnackNotificationComponent,
  ]
})
export class SnackNotificationModule { }
