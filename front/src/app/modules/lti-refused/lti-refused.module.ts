import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { LtiRefusedRoutingModule } from './lti-refused-routing.module';
import { RefusedComponent } from './components/refused/refused.component';

/**
 * Kept out of LtiModule so the page renders without the admin layout: the refused
 * administrator has no session, often inside the platform's iframe.
 */
@NgModule({
  declarations: [
    RefusedComponent
  ],
  imports: [
    CommonModule,
    LtiRefusedRoutingModule,
    MatButtonModule,
    TranslateModule
  ]
})
export class LtiRefusedModule { }
