import { Route } from '@angular/router';

export interface Breadcrumb {
  displayName: string;
  terminal: boolean;
  url: string;
  route: Route | null;
  args: { [key: string]: string };
}
