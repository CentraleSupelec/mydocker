import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from "@angular/forms";
import { FlexLayoutModule } from "@angular/flex-layout";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatIconModule } from "@angular/material/icon";
import { SessionsFormComponent } from "./components/sessions-form/sessions-form.component";
import { SessionFormComponent } from "./components/session-form/session-form.component";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { DatetimePickerModule } from "../utils/datetime-picker/datetime-picker.module";
import { NgxPermissionsModule } from "ngx-permissions";


@NgModule({
  declarations: [
    SessionFormComponent,
    SessionsFormComponent
  ],
  exports: [
    SessionsFormComponent
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
    NgxPermissionsModule,
  ]
})
export class SessionsFormModule { }
