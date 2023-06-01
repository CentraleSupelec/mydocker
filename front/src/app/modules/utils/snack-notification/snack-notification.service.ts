import { Injectable } from '@angular/core';
import { ISnackData, ToastType } from './toast';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SnackNotificationComponent } from './snack-notification/snack-notification.component';

@Injectable({providedIn: 'root'})
export class SnackNotificationService {
  constructor(
    private readonly snackBar: MatSnackBar,
  ) {
  }

  push(message: string, type: ToastType) {
    const snackData: ISnackData = {
      message: message,
      type: type,
    };
    this.snackBar.openFromComponent(SnackNotificationComponent, {
      duration: 3000,
      data: snackData,
    });
  }
}
