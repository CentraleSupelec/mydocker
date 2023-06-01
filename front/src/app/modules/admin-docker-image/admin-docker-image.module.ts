import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AdminDockerImageRoutingModule } from './admin-docker-image-routing.module';
import { DockerImageListComponent } from './components/docker-image-list/docker-image-list.component';
import { MatTableModule } from "@angular/material/table";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { FlexLayoutModule } from "@angular/flex-layout";
import { UtilsModule } from "../utils/utils.module";
import { DockerImageBuildStatusComponent } from './components/docker-image-build-status/docker-image-build-status.component';
import { DockerImageEditComponent } from './components/docker-image-edit/docker-image-edit.component';
import { MonacoEditorModule } from "ngx-monaco-editor";
import { DockerImageFormComponent } from './components/docker-image-form/docker-image-form.component';
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatStepperModule } from "@angular/material/stepper";
import { MatTabsModule } from "@angular/material/tabs";
import { PortsFormModule } from "../ports-form/ports-form.module";
import { FileInputModule } from "../file-input/file-input.module";
import { DockerImageCreateComponent } from './components/docker-image-create/docker-image-create.component';
import { DockerImageDetailComponent } from './components/docker-image-detail/docker-image-detail.component';
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { DisplayContainerModule } from "../display-container/display-container.module";
import { DockerImageBuildDetailDialogComponent } from './components/docker-image-build-detail-dialog/docker-image-build-detail-dialog.component';
import { MatDialogModule } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { APP_CONFIG, appConstantFactory } from "../../app-config";
import { SnackNotificationModule } from "../utils/snack-notification/snack-notification.module";
import { PermissionsModule } from "../permissions/permissions.module";
import { NgxPermissionsModule } from "ngx-permissions";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { LogDialogModule } from "../log-dialog/log-dialog.module";
import { MatSortModule } from "@angular/material/sort";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatCardModule } from "@angular/material/card";


@NgModule({
    declarations: [
        DockerImageListComponent,
        DockerImageBuildStatusComponent,
        DockerImageEditComponent,
        DockerImageFormComponent,
        DockerImageCreateComponent,
        DockerImageDetailComponent,
        DockerImageBuildDetailDialogComponent
    ],
    imports: [
        CommonModule,
        AdminDockerImageRoutingModule,
        MatTableModule,
        MatButtonModule,
        MatIconModule,
        MatTooltipModule,
        FlexLayoutModule,
        UtilsModule,
        MonacoEditorModule,
        MatFormFieldModule,
        MatInputModule,
        ReactiveFormsModule,
        MatStepperModule,
        MatTabsModule,
        PortsFormModule,
        FileInputModule,
        MatProgressSpinnerModule,
        DisplayContainerModule,
        MatDialogModule,
        MatDividerModule,
        SnackNotificationModule,
        PermissionsModule,
        NgxPermissionsModule,
        MatCheckboxModule,
        LogDialogModule,
        MatSortModule,
        MatPaginatorModule,
        MatCardModule,
        FormsModule
    ],
    providers: [
        {
            provide: APP_CONFIG,
            useFactory: appConstantFactory
        },
    ],
    exports: [
        DockerImageBuildStatusComponent
    ]
})
export class AdminDockerImageModule { }
