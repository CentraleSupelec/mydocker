import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ClustersResolver } from './resolvers/clusters.resolver';
import { ClusterListComponent } from './components/cluster-list/cluster-list.component';
import { ClusterNewComponent } from './components/cluster-new/cluster-new.component';
import { ClusterEditComponent } from './components/cluster-edit/cluster-edit.component';
import { ClusterResolver } from './resolvers/cluster.resolver';

const routes: Routes = [
  {
    path: '',
    component: ClusterListComponent,
    resolve: {
      clusters: ClustersResolver,
    }
  },
  {
    path: 'new',
    component: ClusterNewComponent,
    data: {
      breadcrumb: 'admin.resources_management.clusters.create_breadcrumb',
    }
  },
  {
    path: ':id/edit',
    component: ClusterEditComponent,
    resolve: {
      cluster: ClusterResolver,
    },
    data: {
      breadcrumb: 'admin.resources_management.clusters.edit.breadcrumb',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ClusterRoutingModule { }
