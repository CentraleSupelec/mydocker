import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DeploymentCreateComponent } from "./components/deployment-create/deployment-create.component";
import { OvhResourceResolver } from "../sessions-resources/resolvers/ovh-resource.resolver";
import { RegionsResolver } from "../regions/resolvers/regions.resolver";
import { DeploymentListComponent } from "./components/deployment-list/deployment-list.component";
import { DeploymentsResolver } from "./resolvers/deployments.resolver";
import { DeploymentViewComponent } from "./components/deployment-view/deployment-view.component";
import { DeploymentResolver } from "./resolvers/deployment.resolver";
import { ComputeTypesResolver } from '../compute-type/resolvers/compute-types.resolver';

const routes: Routes = [
  {
    path: '',
    component: DeploymentListComponent,
    resolve: {
      deployments: DeploymentsResolver,
      resources: OvhResourceResolver,
      computeTypes: ComputeTypesResolver,
    }
  },
  {
    path: 'create',
    component: DeploymentCreateComponent,
    resolve: {
      resources: OvhResourceResolver,
      regions: RegionsResolver,
      computeTypes: ComputeTypesResolver,
    },
    data: {
      breadcrumb: 'Création d\'un déploiement',
    }
  },
  {
    path: ':id',
    component: DeploymentViewComponent,
    resolve: {
      deployment: DeploymentResolver,
      resources: OvhResourceResolver,
      computeTypes: ComputeTypesResolver,
    },
    data: {
      breadcrumb: 'Visualisation d\'un déploiement',
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DeploymentRoutingModule { }
