import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClusterListComponent } from './components/cluster-list/cluster-list.component';
import { MatTableModule } from '@angular/material/table';
import { FlexModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ClusterRoutingModule } from './cluster-routing.module';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClusterFormComponent } from './components/cluster-form/cluster-form.component';
import { ClusterNewComponent } from './components/cluster-new/cluster-new.component';
import { ClusterEditComponent } from './components/cluster-edit/cluster-edit.component';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { ConfirmDialogModule } from '../utils/confirm-dialog/confirm-dialog.module';
import { MatSortModule } from '@angular/material/sort';
import { SnackNotificationModule } from '../utils/snack-notification/snack-notification.module';
import { SessionsResourcesModule } from '../sessions-resources/sessions-resources.module';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { APP_CONFIG, appConstantFactory } from "../../app-config";
import { TranslateModule } from '@ngx-translate/core';


@NgModule({
  declarations: [
    ClusterListComponent,
    ClusterFormComponent,
    ClusterNewComponent,
    ClusterEditComponent
  ],
  imports: [
    CommonModule,
    MatTableModule,
    FlexModule,
    MatButtonModule,
    MatIconModule,
    ClusterRoutingModule,
    MatTooltipModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatCardModule,
    ConfirmDialogModule,
    MatSortModule,
    SnackNotificationModule,
    SessionsResourcesModule,
    MatSelectModule,
    MatExpansionModule,
    TranslateModule
  ],
  providers: [
    {
      provide: APP_CONFIG,
      useFactory: appConstantFactory
    },
  ],
})
export class ClusterModule { }
