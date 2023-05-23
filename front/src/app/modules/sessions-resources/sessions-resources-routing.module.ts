import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SessionWithResourceListComponent } from "./components/session-with-resource-list/session-with-resource-list.component";
import { SessionWithResourceEditComponent } from "./components/session-with-resource-edit/session-with-resource-edit.component";
import { SessionWithResourceResolver } from "./resolvers/session-with-resource.resolver";
import { OvhResourceResolver } from "./resolvers/ovh-resource.resolver";

const routes: Routes = [
  {
    path: '',
    component: SessionWithResourceListComponent,
  },
  {
    path: ':id/edit',
    component: SessionWithResourceEditComponent,
    resolve: {
      session: SessionWithResourceResolver,
      resources: OvhResourceResolver,
    },
    data: {
      breadcrumb: 'Ajout des ressources',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class SessionsResourcesRoutingModule { }
