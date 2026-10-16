import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DeploymentStatusDetailComponent } from "./components/deployment-status-detail/deployment-status-detail.component";
import { ClustersBasicInformationResolver } from "../cluster/resolvers/clusters-basic-information.resolver";

const routes: Routes = [
  {
    path: '',
    component: DeploymentStatusDetailComponent,
    resolve: {
      clusters: ClustersBasicInformationResolver
    },
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DeploymentStatusRoutingModule { }
