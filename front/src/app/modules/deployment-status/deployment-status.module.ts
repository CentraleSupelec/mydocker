import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DeploymentStatusRoutingModule } from './deployment-status-routing.module';
import { DeploymentStatusDetailDialogComponent } from "./components/deployment-status-detail-dialog/deployment-status-detail-dialog.component";
import { DeploymentStatusDetailComponent } from "./components/deployment-status-detail/deployment-status-detail.component";
import { MatDialogModule } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { FlexModule } from "@angular/flex-layout";
import { MatDividerModule } from "@angular/material/divider";
import { MatTableModule } from "@angular/material/table";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { DeploymentStatusStatusComponent } from "./components/deployment-status-status/deployment-status-status.component";
import { MatPaginatorModule } from "@angular/material/paginator";
import { SnackNotificationModule } from "../utils/snack-notification/snack-notification.module";


@NgModule({
  declarations: [
    DeploymentStatusDetailDialogComponent,
    DeploymentStatusDetailComponent,
    DeploymentStatusStatusComponent
  ],
    imports: [
        CommonModule,
        DeploymentStatusRoutingModule,
        MatDialogModule,
        MatButtonModule,
        FlexModule,
        MatDividerModule,
        MatTableModule,
        MatProgressSpinnerModule,
        MatIconModule,
        MatTooltipModule,
        MatPaginatorModule,
        SnackNotificationModule
    ]
})
export class DeploymentStatusModule { }
