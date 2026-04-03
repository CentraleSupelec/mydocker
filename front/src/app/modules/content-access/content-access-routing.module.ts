import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ContentShowComponent } from "./components/content-show/content-show.component";
import { ContentResolver } from './resolvers/content.resolver';

const routes: Routes = [
  // {
  //   path: 'join/:link',
  //   component: CourseJoinComponent,
  //   children: [
  //     {
  //       path: 'hub/user-redirect/:user_redirect',
  //       component: CourseJoinComponent
  //     },
  //     {
  //       path: 'user-redirect/:user_redirect',
  //       component: CourseJoinComponent
  //     }
  //   ]
  // },
  {
    path: ':slug',
    component: ContentShowComponent,
    resolve: {
      content: ContentResolver,
    },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ContentAccessRoutingModule { }
