import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BreadcrumbComponent } from './breadcrumb/breadcrumb.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import { FlexModule } from '@angular/flex-layout';
import { UtilsModule } from "../utils.module";
import { MatButtonModule } from "@angular/material/button";
import { MatTooltipModule } from "@angular/material/tooltip";

@NgModule({
  declarations: [
    BreadcrumbComponent,
  ],
  imports: [
    CommonModule,
    MatToolbarModule,
    RouterModule,
    MatIconModule,
    FlexModule,
    UtilsModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  exports: [
    BreadcrumbComponent,
  ],
})
export class BreadcrumbModule {
}
