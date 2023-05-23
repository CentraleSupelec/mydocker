import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DeploymentRoutingModule } from './deployment-routing.module';
import { DeploymentCreateComponent } from './components/deployment-create/deployment-create.component';
import { DeploymentFormComponent } from './components/deployment-form/deployment-form.component';
import { MatStepperModule } from "@angular/material/stepper";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { FlexModule } from "@angular/flex-layout";
import { DatetimePickerModule } from "../utils/datetime-picker/datetime-picker.module";
import { LaunchWorkersFormComponent } from './components/launch-workers-form/launch-workers-form.component';
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatIconModule } from "@angular/material/icon";
import { WorkerFormComponent } from './components/worker-form/worker-form.component';
import { MatInputModule } from "@angular/material/input";
import { CleanWorkersFormComponent } from './components/clean-workers-form/clean-workers-form.component';
import { MatCheckboxModule } from "@angular/material/checkbox";
import { DeploymentListComponent } from './components/deployment-list/deployment-list.component';
import { MatTableModule } from "@angular/material/table";
import { DeploymentViewComponent } from './components/deployment-view/deployment-view.component';
import { ConfirmDialogModule } from "../utils/confirm-dialog/confirm-dialog.module";
import { UtilsModule } from "../utils/utils.module";
import { RegionsModule } from '../regions/regions.module';


@NgModule({
  declarations: [
    DeploymentCreateComponent,
    DeploymentFormComponent,
    LaunchWorkersFormComponent,
    WorkerFormComponent,
    CleanWorkersFormComponent,
    DeploymentListComponent,
    DeploymentViewComponent
  ],
    imports: [
        CommonModule,
        DeploymentRoutingModule,
        MatStepperModule,
        MatFormFieldModule,
        MatSelectModule,
        ReactiveFormsModule,
        MatButtonModule,
        FlexModule,
        DatetimePickerModule,
        MatTooltipModule,
        MatIconModule,
        MatInputModule,
        MatCheckboxModule,
        MatTableModule,
        ConfirmDialogModule,
        UtilsModule,
        RegionsModule,
    ]
})
export class DeploymentModule { }
