import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UsersListComponent } from "./components/users-list/users-list.component";
import { UserCreateComponent } from "./components/user-create/user-create.component";
import { UserEditComponent } from "./components/user-edit/user-edit.component";
import { UserResolver } from "./service/user.resolver";

const routes: Routes = [
  {
    path: '',
    component: UsersListComponent,
  },
  {
    path: 'new',
    component: UserCreateComponent,
    data: {
      breadcrumb: 'Création d\'un utilisateur',
    }
  },
  {
    path: ':id/edit',
    component: UserEditComponent,
    resolve: {
      user: UserResolver,
    },
    data: {
      breadcrumb: 'Édition d\'un utilisateur',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminUsersRoutingModule { }
