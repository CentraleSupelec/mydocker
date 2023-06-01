import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AdminUsersRoutingModule } from './admin-users-routing.module';
import { UsersListComponent } from './components/users-list/users-list.component';
import { MatTableModule } from "@angular/material/table";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatSortModule } from "@angular/material/sort";
import { FlexLayoutModule } from "@angular/flex-layout";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatIconModule } from "@angular/material/icon";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatCardModule } from "@angular/material/card";
import { UserFormComponent } from './components/user-form/user-form.component';
import { MatSelectModule } from "@angular/material/select";
import { UserCreateComponent } from './components/user-create/user-create.component';
import { UserEditComponent } from './components/user-edit/user-edit.component';
import { ObservableSnackNotificationService } from "../utils/snack-notification/observable-snack-notification.service";
import { SnackNotificationModule } from "../utils/snack-notification/snack-notification.module";


@NgModule({
  declarations: [
    UsersListComponent,
    UserFormComponent,
    UserCreateComponent,
    UserEditComponent
  ],
  imports: [
    CommonModule,
    AdminUsersRoutingModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    FlexLayoutModule,
    MatButtonModule,
    MatTooltipModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    FormsModule,
    MatCardModule,
    MatSelectModule,
    SnackNotificationModule,
  ]
})
export class AdminUsersModule { }
