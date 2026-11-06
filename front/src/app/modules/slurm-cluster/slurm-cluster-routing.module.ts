import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SlurmClustersDetailsResolver } from './resolvers/slurm-clusters-details.resolver';
import { SlurmClusterListComponent } from './components/slurm-cluster-list/slurm-cluster-list.component';
import { SlurmClusterNewComponent } from './components/slurm-cluster-new/slurm-cluster-new.component';
import { SlurmClusterEditComponent } from './components/slurm-cluster-edit/slurm-cluster-edit.component';
import { SlurmClusterResolver } from './resolvers/slurm-cluster.resolver';

const routes: Routes = [
  {
    path: '',
    component: SlurmClusterListComponent,
    resolve: {
      slurmClusters: SlurmClustersDetailsResolver,
    },
  },
  {
    path: 'new',
    component: SlurmClusterNewComponent,
    data: {
      breadcrumb: 'admin.resources_management.slurm_clusters.create_breadcrumb',
    },
  },
  {
    path: ':id/edit',
    component: SlurmClusterEditComponent,
    resolve: {
      slurmCluster: SlurmClusterResolver,
    },
    data: {
      breadcrumb: 'admin.resources_management.slurm_clusters.edit.breadcrumb',
    },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SlurmClusterRoutingModule {}
