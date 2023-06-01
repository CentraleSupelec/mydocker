import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CourseEditComponent } from "./components/course-edit/course-edit.component";
import { AdminCourseResolver } from "./resolvers/admin-course.resolver";
import { CourseNewComponent } from "./components/course-new/course-new.component";
import { CoursesAdminComponent } from './components/courses-admin/courses-admin.component';
import { ComputeTypesResolver } from '../compute-type/resolvers/compute-types.resolver';

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
    },
    data: {
      breadcrumb: 'Création d\'un cours',
    }
  },
  {
    path: ':id/edit',
    component: CourseEditComponent,
    resolve: {
      course: AdminCourseResolver,
      computeTypes: ComputeTypesResolver,
    },
    data: {
      breadcrumb: 'Édition d\'un cours',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminCourseRoutingModule {
}
