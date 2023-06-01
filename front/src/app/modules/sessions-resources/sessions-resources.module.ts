import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SessionsResourcesRoutingModule } from './sessions-resources-routing.module';
import { SessionWithResourceListComponent } from './components/session-with-resource-list/session-with-resource-list.component';
import { MatTableModule } from "@angular/material/table";
import { FlexModule } from "@angular/flex-layout";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { UtilsModule } from "../utils/utils.module";
import { MatSortModule } from "@angular/material/sort";
import { DatetimePickerModule } from "../utils/datetime-picker/datetime-picker.module";
import { ReactiveFormsModule } from "@angular/forms";
import { SessionWithResourceEditComponent } from './components/session-with-resource-edit/session-with-resource-edit.component';
import { SessionsResourcesFormModule } from "../sessions-resources-form/sessions-form.module";


@NgModule({
  declarations: [
    SessionWithResourceListComponent,
    SessionWithResourceEditComponent
  ],
  imports: [
    CommonModule,
    SessionsResourcesRoutingModule,
    MatTableModule,
    FlexModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    UtilsModule,
    MatSortModule,
    DatetimePickerModule,
    ReactiveFormsModule,
    SessionsResourcesFormModule,
  ]
})
export class SessionsResourcesModule { }
