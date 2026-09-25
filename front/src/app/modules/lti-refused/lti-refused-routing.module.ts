import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RefusedComponent } from './components/refused/refused.component';

const routes: Routes = [
  {
    // No guard: the back end sends a refused launch here, and that browser has no session.
    path: '',
    component: RefusedComponent,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class LtiRefusedRoutingModule { }
