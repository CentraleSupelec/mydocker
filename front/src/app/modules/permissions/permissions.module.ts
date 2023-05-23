import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DockerImagePermissionDialogComponent } from './components/docker-image-permission-dialog/docker-image-permission-dialog.component';
import { MatDialogModule } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { SearchUserComponent } from './components/search-user/search-user.component';
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatInputModule } from "@angular/material/input";
import { FlexModule } from "@angular/flex-layout";
import { ReactiveFormsModule } from "@angular/forms";
import { PermissionFormComponent } from './components/permission-form/permission-form.component';
import { MatSelectModule } from "@angular/material/select";
import { MatIconModule } from "@angular/material/icon";
import { CoursePermissionDialogComponent } from './components/course-permission-dialog/course-permission-dialog.component';



@NgModule({
  declarations: [
    DockerImagePermissionDialogComponent,
    SearchUserComponent,
    PermissionFormComponent,
    CoursePermissionDialogComponent,
  ],
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatAutocompleteModule,
    MatInputModule,
    FlexModule,
    ReactiveFormsModule,
    MatSelectModule,
    MatIconModule,
  ]
})
export class PermissionsModule { }
