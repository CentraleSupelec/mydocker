import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RegisterComponent } from './components/register/register.component';
import { DeepLinkingComponent } from './components/deep-linking/deep-linking.component';
import { AuthGuard } from '../authentication/services/auth.guard';
import { NgxPermissionsGuard } from 'ngx-permissions';
import { AutologinGuard } from '../authentication/services/autologin.guard';

const routes: Routes = [
  {
    path: 'register',
    component: RegisterComponent,
    canActivate: [AuthGuard, NgxPermissionsGuard],
    data: {
      permissions: {
        only: ['ROLE_ADMIN'],
      }
    }
  },
  {
    path: 'deep-linking',
    component: DeepLinkingComponent,
    canActivate: [AutologinGuard],
    data: {
      permissions: {
        only: ['ROLE_TEACHER', 'ROLE_ADMIN'],
      },
      breadcrumb: 'Lier un environnement MyDocker à une activité du cours',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LtiRoutingModule { }
