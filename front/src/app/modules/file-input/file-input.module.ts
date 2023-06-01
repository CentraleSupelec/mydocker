import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileInputFormComponent } from './file-input-form/file-input-form.component';
import { MatButtonModule } from "@angular/material/button";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatIconModule } from "@angular/material/icon";
import { FileDropDirective } from "./file-drop.directive";
import { FileSelectDirective } from "./file-select.directive";



@NgModule({
  declarations: [
    FileInputFormComponent,
    FileDropDirective,
    FileSelectDirective,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
  ],
  exports: [
    FileInputFormComponent,
    FileDropDirective,
    FileSelectDirective,
  ]
})
export class FileInputModule { }
