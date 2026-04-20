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
      breadcrumb: 'admin.resources_management.compute_types.create_breadcrumb',
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
      breadcrumb: 'admin.resources_management.compute_types.edit.breadcrumb',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ComputeTypeRoutingModule { }
