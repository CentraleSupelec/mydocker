import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DockerImageListComponent } from "./components/docker-image-list/docker-image-list.component";
import { DockerImageEditComponent } from "./components/docker-image-edit/docker-image-edit.component";
import { DockerImageResolver } from "./resolvers/docker-image.resolver";
import { DockerImageCreateComponent } from "./components/docker-image-create/docker-image-create.component";
import { DockerImageDetailComponent } from "./components/docker-image-detail/docker-image-detail.component";

const routes: Routes = [
  {
    path: '',
    component: DockerImageListComponent,
  },
  {
    path: 'new',
    component: DockerImageCreateComponent,
    data: {
      breadcrumb: 'Création d\'une nouvelle image docker',
    }
  },
  {
    path: ':id',
    data: {
      breadcrumb: 'Détail d\'une image docker',
    },
    children: [
      {
        path: '',
        component: DockerImageDetailComponent,
        resolve: {
          docker_image: DockerImageResolver,
        },
      },
      {
        path: 'edit',
        component: DockerImageEditComponent,
        resolve: {
          docker_image: DockerImageResolver,
        },
        data: {
          breadcrumb: 'Édition d\'une image docker',
        }
      },
    ]
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminDockerImageRoutingModule { }
