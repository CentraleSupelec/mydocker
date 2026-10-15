import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DeploymentStatusDetailComponent } from "./components/deployment-status-detail/deployment-status-detail.component";

const routes: Routes = [
  {
    path: '',
    component: DeploymentStatusDetailComponent,
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DeploymentStatusRoutingModule { }
