import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DisplayTerraformStateComponent } from "./components/display-terraform-state/display-terraform-state.component";
import { TerraformStateResolver } from "./resolvers/terraform-state.resolver";

const routes: Routes = [
  {
    path: '',
    component: DisplayTerraformStateComponent,
    resolve: {
      state: TerraformStateResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TerraformStateRoutingModule { }
