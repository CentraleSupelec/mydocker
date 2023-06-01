import { filter, startWith, takeUntil } from 'rxjs/operators';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRouteSnapshot, Event, NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { Breadcrumb } from '../breadcrumb';

@Component({
  selector: 'app-breadcrumb',
  templateUrl: './breadcrumb.component.html',
  styleUrls: ['./breadcrumb.component.scss'],
})
export class BreadcrumbComponent implements OnInit, OnDestroy {
  breadcrumbs: Breadcrumb[] = [];

  private readonly destroy$: Subject<void> = new Subject<void>();

  constructor(
    private readonly router: Router,
  ) {
  }

  ngOnInit(): void {
    this.router.events
      .pipe(
        takeUntil(this.destroy$),
        filter(event => event instanceof NavigationEnd),
        startWith(null)
      )
      .subscribe((routeEvent) => {
      this.onRouteEvent(routeEvent as NavigationEnd);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  private onRouteEvent(routeEvent: NavigationEnd | null) {
    let route = this.router.routerState.root.snapshot;
    let url = '';
    let breadCrumbIndex = 0;
    const newCrumbs = [];

    while (route.firstChild != null) {
      route = route.firstChild;

      if (route.routeConfig === null) {
        continue;
      }
      if (!route.routeConfig.path) {
        continue;
      }

      url += `/${this.createUrl(route)}`;

      if (!route.data['breadcrumb']) {
        continue;
      }

      const newCrumb = this.createBreadcrumb(route, url);

      if (breadCrumbIndex < this.breadcrumbs.length) {
        const existing = this.breadcrumbs[breadCrumbIndex++];

        if (existing && existing.route === route.routeConfig) {
          newCrumb.displayName = existing.displayName;
        }
      }

      newCrumbs.push(newCrumb);
    }

    this.breadcrumbs = newCrumbs;
  }

  private createBreadcrumb(route: ActivatedRouteSnapshot, url: string): Breadcrumb {
    const translationArgs: {  [key: string]: string } = {};
    if (route.data.hasOwnProperty('breadcrumb_keys')) {
      for (const key of route.data['breadcrumb_keys']) {
        translationArgs[key] = route.data[key];
      }
    }

    return {
      displayName: route.data['breadcrumb'],
      terminal: this.isTerminal(route),
      url: url,
      route: route.routeConfig,
      args: translationArgs,
    };
  }

  // noinspection JSMethodCanBeStatic
  private isTerminal(route: ActivatedRouteSnapshot) {
    return route.firstChild === null
      || route.firstChild.routeConfig === null
      || !route.firstChild.routeConfig.path;
  }

  // noinspection JSMethodCanBeStatic
  private createUrl(route: ActivatedRouteSnapshot) {
    return route.url.map(function (s) {
      return s.toString();
    }).join('/');
  }
}
