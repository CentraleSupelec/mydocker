import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CourseEditComponent } from "./components/course-edit/course-edit.component";
import { AdminCourseResolver } from "./resolvers/admin-course.resolver";
import { CourseNewComponent } from "./components/course-new/course-new.component";
import { CoursesAdminComponent } from './components/courses-admin/courses-admin.component';
import { ComputeTypesResolver } from '../compute-type/resolvers/compute-types.resolver';
// Basic information only: these pages are open to teachers, the full cluster list is admin-only.
import { ClustersBasicInformationResolver } from '../cluster/resolvers/clusters-basic-information.resolver';
import { SlurmClustersResolver } from '../slurm-cluster/resolvers/slurm-clusters.resolver'

const routes: Routes = [
  {
    path: '',
    component: CoursesAdminComponent,
  },
  {
    path: 'new',
    component: CourseNewComponent,
    resolve: {
      computeTypes: ComputeTypesResolver,
      clusters: ClustersBasicInformationResolver,
      slurmClusters: SlurmClustersResolver
    },
    data: {
      breadcrumb: 'admin.courses.create_breadcrumb',
    }
  },
  {
    path: ':id/edit',
    component: CourseEditComponent,
    resolve: {
      course: AdminCourseResolver,
      computeTypes: ComputeTypesResolver,
      clusters: ClustersBasicInformationResolver,
      slurmClusters: SlurmClustersResolver
    },
    data: {
      breadcrumb: 'admin.courses.edit.breadcrumb',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminCourseRoutingModule {
}
