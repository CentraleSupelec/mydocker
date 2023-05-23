import { NgModule } from '@angular/core';
import { NgProgressComponent, NgProgressModule } from "ngx-progressbar";
import { NgProgressRouterModule } from "ngx-progressbar/router";
import { NgProgressHttpModule } from "ngx-progressbar/http";


@NgModule({
  imports: [
    NgProgressModule,
    NgProgressRouterModule,
    NgProgressHttpModule,
  ],
  exports: [
    NgProgressComponent,
  ]
})
export class ProgressBarModule { }
