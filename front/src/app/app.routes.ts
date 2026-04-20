import { Routes } from "@angular/router";
import { BasicLayoutComponent } from "./modules/user-interface/components/basic-layout/basic-layout.component";
import { SignInComponent } from "./modules/authentication/components/sign-in/sign-in.component";
import { AuthGuard } from "./modules/authentication/services/auth.guard";
import { LoginAcceptComponent } from "./modules/authentication/components/login-accept/login-accept.component";
import { NgxPermissionsGuard } from "ngx-permissions";
import { AdminLayoutComponent } from "./modules/user-interface/components/admin-layout/admin-layout.component";
import { AutologinGuard } from './modules/authentication/services/autologin.guard';
import { LoginGuard } from "./modules/authentication/services/login.guard";

export const routes: Routes = [
  {
    path: 'login',
    component: SignInComponent,
    canActivate: [LoginGuard]
  },
  {
    path: 'loginAccept',
    component: LoginAcceptComponent,
  },
  {
    path: 'content',
    component: BasicLayoutComponent,
    canActivate: [AutologinGuard],
    loadChildren: () => import('./modules/content-access/content-access.module').then(m => m.ContentAccessModule),
    data: {
      permissions: {
        only: 'ROLE_USER',
      }
    },
  },
  {
    path: 'shell',
    component: BasicLayoutComponent,
    canActivate: [AutologinGuard],
    loadChildren: () => import('./modules/shell/shell.module').then(m => m.ShellModule),
    data: {
      permissions: {
        only: 'ROLE_USER',
      }
    },
  },
  {
    path: 'course/:courseUuid/magic-link',
    component: SignInComponent,
    data: { isMagicLink: true }
  },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [AuthGuard, NgxPermissionsGuard],
    data: {
      permissions: {
        only: ['ROLE_TEACHER', 'ROLE_ADMIN'],
      }
    },
    children: [
      {
        path: 'courses',
        loadChildren: () => import('./modules/admin-course/admin-course.module').then(m => m.AdminCourseModule),
        data: {
          breadcrumb: 'admin.courses.breadcrumb',
        }
      },
      {
        path: 'images',
        loadChildren: () => import('./modules/admin-docker-image/admin-docker-image.module').then(m => m.AdminDockerImageModule),
        data: {
          breadcrumb: 'admin.docker_images.breadcrumb',
        }
      },
      {
        path: 'sessions-resources',
        loadChildren: () => import('./modules/sessions-resources/sessions-resources.module').then(m => m.SessionsResourcesModule),
        data: {
          breadcrumb: 'admin.resources_management.resources_by_session.breadcrumb',
          permissions: {
            only: ['ROLE_ADMIN'],
          }
        }
      },
      {
        path: 'state',
        loadChildren: () => import('./modules/terraform-state/terraform-state.module').then(m => m.TerraformStateModule),
        data: {
          breadcrumb: 'admin.resources_management.infrastructure_state.breadcrumb',
          permissions: {
            only: ['ROLE_ADMIN'],
          }
        }
      },
      {
        path: 'deployment',
        loadChildren: () => import('./modules/deployment/deployment.module').then(m => m.DeploymentModule),
        data: {
          breadcrumb: 'admin.resources_management.deployment.breadcrumb',
          permissions: {
            only: ['ROLE_ADMIN'],
          }
        }
      },
      {
        path: 'deployment-status',
        loadChildren: () => import('./modules/deployment-status/deployment-status.module').then(m => m.DeploymentStatusModule),
        data: {
          breadcrumb: 'admin.resources_management.deployment_status.breadcrumb',
          permissions: {
            only: ['ROLE_ADMIN'],
          }
        }
      },
      {
        path: 'compute-types',
        loadChildren: () => import('./modules/compute-type/compute-type.module').then(m => m.ComputeTypeModule),
        data: {
          breadcrumb: 'admin.resources_management.compute_types.breadcrumb',
          permissions: {
            only: ['ROLE_ADMIN'],
          }
        }
      },
      {
        path: 'users',
        loadChildren: () => import('./modules/admin-users/admin-users.module').then(m => m.AdminUsersModule),
        data: {
          breadcrumb: 'admin.users_management.users.breadcrumb',
          permissions: {
            only: ['ROLE_ADMIN'],
          }
        }
      },
      {
        path: 'contents',
        loadChildren: () => import('./modules/content/content.module').then(m => m.ContentModule),
        data: {
          breadcrumb: 'admin.resources_management.content_management.breadcrumb',
          permissions: {
            only: ['ROLE_ADMIN'],
          }
        }
      },
      {
        path: '**',
        redirectTo: 'courses'
      },
    ]
  },
  {
    path: 'lti',
    component: AdminLayoutComponent,
    children: [
      {
        path: '',
        loadChildren: () => import('./modules/lti/lti.module').then(m => m.LtiModule)
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'shell'
  },
];
