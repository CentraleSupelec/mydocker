import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CourseJoinComponent } from "./components/course-join/course-join.component";
import { CourseListComponent } from "./components/course-list/course-list.component";
import { UserCourseSessionsResolver } from "./resolvers/user-courses-resolver.service";
import { ConnexionHelperComponent } from "./components/connexion-helper/connexion-helper.component";
import { CourseSessionsResolver } from "./resolvers/courses-resolver.service";

const routes: Routes = [
  {
    path: 'join/:link',
    component: CourseJoinComponent,
    children: [
      {
        path: 'hub/user-redirect/:user_redirect',
        component: CourseJoinComponent
      },
      {
        path: 'user-redirect/:user_redirect',
        component: CourseJoinComponent
      }
    ]
  },
  {
    path: '',
    component: CourseListComponent,
    resolve: {
      sessions: UserCourseSessionsResolver,
      courses: CourseSessionsResolver,
    },
  },
  {
    path: 'help',
    component: ConnexionHelperComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ShellRoutingModule { }
