import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContentListComponent } from './components/content-list/content-list.component';
import { MatTableModule } from '@angular/material/table';
import { FlexModule } from '@angular/flex-layout';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ContentRoutingModule } from './content-routing.module';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ContentFormComponent } from './components/content-form/content-form.component';
import { ContentNewComponent } from './components/content-new/content-new.component';
import { ContentEditComponent } from './components/content-edit/content-edit.component';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatCardModule } from '@angular/material/card';
import { ConfirmDialogModule } from '../utils/confirm-dialog/confirm-dialog.module';
import { MatSortModule } from '@angular/material/sort';
import { SnackNotificationModule } from '../utils/snack-notification/snack-notification.module';
import { SessionsResourcesModule } from '../sessions-resources/sessions-resources.module';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { RegionsModule } from '../regions/regions.module';
import { APP_CONFIG, appConstantFactory } from "../../app-config";
import { UtilsModule } from '../utils/utils.module';


@NgModule({
  declarations: [
    ContentListComponent,
    ContentFormComponent,
    ContentNewComponent,
    ContentEditComponent
  ],
  imports: [
    CommonModule,
    UtilsModule,
    MatTableModule,
    FlexModule,
    MatButtonModule,
    MatIconModule,
    ContentRoutingModule,
    MatTooltipModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatCardModule,
    ConfirmDialogModule,
    MatSortModule,
    SnackNotificationModule,
    SessionsResourcesModule,
    MatSelectModule,
    MatExpansionModule,
    RegionsModule,
  ],
  providers: [
    {
      provide: APP_CONFIG,
      useFactory: appConstantFactory
    },
  ],
})
export class ContentModule { }
