import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from "@angular/forms";
import { FlexLayoutModule } from "@angular/flex-layout";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatIconModule } from "@angular/material/icon";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { DatetimePickerModule } from "../utils/datetime-picker/datetime-picker.module";
import { ResourceFormComponent } from "./components/resource-form/resource-form.component";
import { ResourcesFormComponent } from "./components/resources-form/resources-form.component";
import { MatSelectModule } from "@angular/material/select";


@NgModule({
  declarations: [
    ResourceFormComponent,
    ResourcesFormComponent,
  ],
  exports: [
    ResourcesFormComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTooltipModule,
    MatIconModule,
    MatCheckboxModule,
    DatetimePickerModule,
    MatSelectModule,
  ]
})
export class SessionsResourcesFormModule { }
