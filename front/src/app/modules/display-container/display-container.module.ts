import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DisplayContainerComponent } from "./display-container/display-container.component";
import { DisplayContainerPortComponent } from "./display-container-port/display-container-port.component";
import { HttpConnectionGuideComponent } from "./http-connection-guide/http-connection-guide.component";
import { SshConnectionGuideComponent } from "./ssh-connection-guide/ssh-connection-guide.component";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { FlexLayoutModule } from "@angular/flex-layout";
import { RouterModule } from "@angular/router";
import { SnackNotificationModule } from "../utils/snack-notification/snack-notification.module";
import { DisplayCustomContainerPortComponent } from './display-custom-container-port/display-custom-container-port.component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RenderCreationErrorPipe } from "./render-creation-error.pipe";



@NgModule({
  declarations: [
    DisplayContainerComponent,
    DisplayContainerPortComponent,
    HttpConnectionGuideComponent,
    SshConnectionGuideComponent,
    DisplayCustomContainerPortComponent,
    RenderCreationErrorPipe,
  ],
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    FlexLayoutModule,
    RouterModule,
    SnackNotificationModule,
  ],
  exports: [
    DisplayContainerComponent,
  ]
})
export class DisplayContainerModule { }
