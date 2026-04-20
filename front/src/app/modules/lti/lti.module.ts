import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RegisterComponent } from './components/register/register.component';
import { LtiRoutingModule } from './lti-routing.module';
import { DeepLinkingComponent } from './components/deep-linking/deep-linking.component';
import { MatButtonModule } from '@angular/material/button';
import { AdminCourseModule } from '../admin-course/admin-course.module';
import {MatTooltipModule} from "@angular/material/tooltip";
import { TranslateModule } from '@ngx-translate/core';



@NgModule({
  declarations: [
    RegisterComponent,
    DeepLinkingComponent
  ],
    imports: [
        CommonModule,
        LtiRoutingModule,
        MatButtonModule,
        AdminCourseModule,
        MatTooltipModule,
        TranslateModule
    ]
})
export class LtiModule { }
