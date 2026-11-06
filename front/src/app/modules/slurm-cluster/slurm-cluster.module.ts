import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SlurmClusterListComponent } from './components/slurm-cluster-list/slurm-cluster-list.component';
import { MatTableModule } from '@angular/material/table';
import { FlexModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SlurmClusterRoutingModule } from './slurm-cluster-routing.module';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SlurmClusterFormComponent } from './components/slurm-cluster-form/slurm-cluster-form.component';
import { SlurmClusterNewComponent } from './components/slurm-cluster-new/slurm-cluster-new.component';
import { SlurmClusterEditComponent } from './components/slurm-cluster-edit/slurm-cluster-edit.component';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { ConfirmDialogModule } from '../utils/confirm-dialog/confirm-dialog.module';
import { MatSortModule } from '@angular/material/sort';
import { SnackNotificationModule } from '../utils/snack-notification/snack-notification.module';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { APP_CONFIG, appConstantFactory } from '../../app-config';
import { TranslateModule } from '@ngx-translate/core';

@NgModule({
  declarations: [
    SlurmClusterListComponent,
    SlurmClusterFormComponent,
    SlurmClusterNewComponent,
    SlurmClusterEditComponent,
  ],
  imports: [
    CommonModule,
    MatTableModule,
    FlexModule,
    MatButtonModule,
    MatIconModule,
    SlurmClusterRoutingModule,
    MatTooltipModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatCardModule,
    ConfirmDialogModule,
    MatSortModule,
    SnackNotificationModule,
    MatSelectModule,
    MatExpansionModule,
    TranslateModule,
  ],
  providers: [
    {
      provide: APP_CONFIG,
      useFactory: appConstantFactory,
    },
  ],
})
export class SlurmClusterModule {}
