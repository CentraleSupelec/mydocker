import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AdminCourseRoutingModule } from './admin-course-routing.module';
import { CoursesListComponent } from './components/courses-list/courses-list.component';
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { FlexModule } from "@angular/flex-layout";
import { MatTooltipModule } from "@angular/material/tooltip";
import { CourseEditComponent } from './components/course-edit/course-edit.component';
import { UtilsModule } from "../utils/utils.module";
import { CourseFormComponent } from './components/course-form/course-form.component';
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatInputModule } from "@angular/material/input";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { PortsFormModule } from "../ports-form/ports-form.module";
import { CourseNewComponent } from './components/course-new/course-new.component';
import { NgxPermissionsModule } from "ngx-permissions";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { DisplayContainerModule } from "../display-container/display-container.module";
import { APP_CONFIG, appConstantFactory } from "../../app-config";
import { SnackNotificationModule } from "../utils/snack-notification/snack-notification.module";
import { ConfirmDialogModule } from "../utils/confirm-dialog/confirm-dialog.module";
import { MatSortModule } from "@angular/material/sort";
import { CourseGeneralInformationFormComponent } from './components/course-general-information-form/course-general-information-form.component';
import { DatetimePickerModule } from "../utils/datetime-picker/datetime-picker.module";
import { SessionsFormModule } from "../sessions-form/sessions-form.module";
import { CourseTechnicalInformationFormComponent } from './components/course-technical-information-form/course-technical-information-form.component';
import { MatStepperModule } from "@angular/material/stepper";
import { DockerImageChoiceDialogComponent } from './components/docker-image-choice-dialog/docker-image-choice-dialog.component';
import { MatDialogModule } from "@angular/material/dialog";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MatOptionModule } from "@angular/material/core";
import { MatSelectModule } from "@angular/material/select";
import { AdminDockerImageModule } from "../admin-docker-image/admin-docker-image.module";
import { MatDividerModule } from "@angular/material/divider";
import { MatExpansionModule } from "@angular/material/expansion";
import { CourseDisplayFormComponent } from './components/course-display-form/course-display-form.component';
import { CourseDisplayPortsFormComponent } from './components/course-display-ports-form/course-display-ports-form.component';
import { CourseDisplayCustomUrlComponent } from './components/course-display-custom-url/course-display-custom-url.component';
import { CourseDisplayUrlComponent } from './components/course-display-url/course-display-url.component';
import { CourseIconStatusComponent } from "./components/course-icon-status/course-icon-status.component";
import { MatCardModule } from "@angular/material/card";
import { MatPaginatorModule } from "@angular/material/paginator";
import { CoursesAdminComponent } from './components/courses-admin/courses-admin.component';
import { ComputeTypeModule } from '../compute-type/compute-type.module';
import { GenerateJoinLinkPipe } from "../utils/generate-join-link.pipe";


@NgModule({
  declarations: [
    CoursesListComponent,
    CourseEditComponent,
    CourseFormComponent,
    CourseNewComponent,
    CourseGeneralInformationFormComponent,
    CourseTechnicalInformationFormComponent,
    DockerImageChoiceDialogComponent,
    CourseDisplayFormComponent,
    CourseDisplayPortsFormComponent,
    CourseDisplayPortsFormComponent,
    CourseDisplayCustomUrlComponent,
    CourseDisplayUrlComponent,
    CourseIconStatusComponent,
    CoursesAdminComponent,
  ],
  imports: [
    CommonModule,
    AdminCourseRoutingModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    FlexModule,
    MatTooltipModule,
    UtilsModule,
    ReactiveFormsModule,
    MatInputModule,
    MatCheckboxModule,
    PortsFormModule,
    NgxPermissionsModule,
    MatProgressSpinnerModule,
    DisplayContainerModule,
    SnackNotificationModule,
    ConfirmDialogModule,
    MatSortModule,
    DatetimePickerModule,
    SessionsFormModule,
    MatStepperModule,
    MatDialogModule,
    MatAutocompleteModule,
    MatOptionModule,
    MatSelectModule,
    AdminDockerImageModule,
    MatDividerModule,
    MatExpansionModule,
    MatCardModule,
    FormsModule,
    MatPaginatorModule,
    ComputeTypeModule
  ],
  providers: [
    {
      provide: APP_CONFIG,
      useFactory: appConstantFactory
    },
    GenerateJoinLinkPipe,
  ],
  exports: [
    CoursesListComponent
  ]
})
export class AdminCourseModule { }
