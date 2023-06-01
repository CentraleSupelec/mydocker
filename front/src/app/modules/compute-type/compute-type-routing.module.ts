import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ComputeTypesResolver } from './resolvers/compute-types.resolver';
import { ComputeTypeListComponent } from './components/compute-type-list/compute-type-list.component';
import { ComputeTypeNewComponent } from './components/compute-type-new/compute-type-new.component';
import { ComputeTypeEditComponent } from './components/compute-type-edit/compute-type-edit.component';
import { ComputeTypeResolver } from './resolvers/compute-type.resolver';
import { OvhResourceResolver } from '../sessions-resources/resolvers/ovh-resource.resolver';
import { RegionsResolver } from '../regions/resolvers/regions.resolver';



const routes: Routes = [
  {
    path: '',
    component: ComputeTypeListComponent,
    resolve: {
      computeTypes: ComputeTypesResolver,
    }
  },
  {
    path: 'new',
    component: ComputeTypeNewComponent,
    resolve: {
      resources: OvhResourceResolver,
      regions: RegionsResolver,
    },
    data: {
      breadcrumb: 'Création d\'un type de charge',
    }
  },
  {
    path: ':id/edit',
    component: ComputeTypeEditComponent,
    resolve: {
      resources: OvhResourceResolver,
      regions: RegionsResolver,
      computeType: ComputeTypeResolver,
    },
    data: {
      breadcrumb: 'Édition d\'un type de charge',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ComputeTypeRoutingModule { }
