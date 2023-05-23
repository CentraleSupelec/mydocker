import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PortFormComponent } from "./components/port-form/port-form.component";
import { PortsFormComponent } from "./components/ports-form/ports-form.component";
import { ReactiveFormsModule } from "@angular/forms";
import { FlexLayoutModule } from "@angular/flex-layout";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatIconModule } from "@angular/material/icon";
import { MatSelectModule } from "@angular/material/select";
import { MatCheckboxModule } from "@angular/material/checkbox";



@NgModule({
  declarations: [
    PortFormComponent,
    PortsFormComponent,
  ],
  exports: [
    PortsFormComponent
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
        MatSelectModule,
        MatCheckboxModule
    ]
})
export class PortsFormModule { }
