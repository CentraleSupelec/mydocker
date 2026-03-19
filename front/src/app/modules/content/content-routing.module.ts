import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ContentsResolver } from './resolvers/contents.resolver';
import { ContentListComponent } from './components/content-list/content-list.component';
import { ContentNewComponent } from './components/content-new/content-new.component';
import { ContentEditComponent } from './components/content-edit/content-edit.component';
import { ContentResolver } from './resolvers/content.resolver';
import { OvhResourceResolver } from '../sessions-resources/resolvers/ovh-resource.resolver';
import { RegionsResolver } from '../regions/resolvers/regions.resolver';



const routes: Routes = [
  {
    path: '',
    component: ContentListComponent,
    resolve: {
      contents: ContentsResolver,
    }
  },
  {
    path: 'new',
    component: ContentNewComponent,
    resolve: {
      resources: OvhResourceResolver,
      regions: RegionsResolver,
    },
    data: {
      breadcrumb: 'Création d\'un contenu',
    }
  },
  {
    path: ':id/edit',
    component: ContentEditComponent,
    resolve: {
      resources: OvhResourceResolver,
      regions: RegionsResolver,
      content: ContentResolver,
    },
    data: {
      breadcrumb: 'Édition d\'un contenu',
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ContentRoutingModule { }
