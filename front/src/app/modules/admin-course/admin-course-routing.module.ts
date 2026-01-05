import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CourseEditComponent } from "./components/course-edit/course-edit.component";
import { AdminCourseResolver } from "./resolvers/admin-course.resolver";
import { CourseNewComponent } from "./components/course-new/course-new.component";
import { CoursesAdminComponent } from './components/courses-admin/courses-admin.component';
import { ComputeTypesResolver } from '../compute-type/resolvers/compute-types.resolver';
import { ClustersResolver } from '../cluster/resolvers/clusters.resolver';

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
      clusters: ClustersResolver
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
      clusters: ClustersResolver
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
