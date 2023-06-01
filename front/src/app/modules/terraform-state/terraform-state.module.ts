import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { TerraformStateRoutingModule } from './terraform-state-routing.module';
import { DisplayTerraformStateComponent } from './components/display-terraform-state/display-terraform-state.component';
import { FlexModule } from "@angular/flex-layout";
import { MatExpansionModule } from "@angular/material/expansion";


@NgModule({
  declarations: [
    DisplayTerraformStateComponent
  ],
  imports: [
    CommonModule,
    TerraformStateRoutingModule,
    FlexModule,
    MatExpansionModule
  ]
})
export class TerraformStateModule { }
