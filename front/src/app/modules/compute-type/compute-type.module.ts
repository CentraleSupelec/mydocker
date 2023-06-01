import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ComputeTypeListComponent } from './components/compute-type-list/compute-type-list.component';
import { MatTableModule } from '@angular/material/table';
import { FlexModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ComputeTypeRoutingModule } from './compute-type-routing.module';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ComputeTypeFormComponent } from './components/compute-type-form/compute-type-form.component';
import { ComputeTypeNewComponent } from './components/compute-type-new/compute-type-new.component';
import { ComputeTypeEditComponent } from './components/compute-type-edit/compute-type-edit.component';
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
import { RegionsModule } from '../regions/regions.module';



@NgModule({
  declarations: [
    ComputeTypeListComponent,
    ComputeTypeFormComponent,
    ComputeTypeNewComponent,
    ComputeTypeEditComponent
  ],
  imports: [
    CommonModule,
    MatTableModule,
    FlexModule,
    MatButtonModule,
    MatIconModule,
    ComputeTypeRoutingModule,
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
    RegionsModule,
  ]
})
export class ComputeTypeModule { }
