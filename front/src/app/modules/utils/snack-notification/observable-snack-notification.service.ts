import { Injectable } from '@angular/core';
import { NavigationExtras, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Location } from '@angular/common';

import { SnackNotificationService } from './snack-notification.service';


@Injectable({providedIn: 'root'})
export class ObservableSnackNotificationService {
  constructor(
    private readonly toastService: SnackNotificationService,
    private readonly router: Router,
    private readonly location: Location,
  ) {
  }

  toast(observable: Observable<any>,
        onSuccessMessage: string,
        onErrorMessage: string,
        goBack: boolean = false,
        redirectTo: string[] = [],
        redirectToExtras: NavigationExtras = {}): void {
    this.toastWithCallback(observable, onSuccessMessage, onErrorMessage, () => {
      if (goBack) {
        this.location.back();
      }
      if (redirectTo) {
        this.router.navigate(redirectTo, redirectToExtras);
      }
    });
  }

  toastWithCallback(observable: Observable<any>, onSuccessMessage: string, onErrorMessage: string, callback: any): void {
    observable.subscribe(
      (result) => {
        this.toastService.push(onSuccessMessage, 'success');
        callback(result);
      },
      (err) => {
        this.toastService.push(onErrorMessage, 'error');
        console.error(err);
      });
  }
}
