import { Injectable } from '@angular/core';
import { SnackNotificationService } from "./snack-notification.service";
import { Clipboard } from "@angular/cdk/clipboard";

@Injectable({providedIn: 'root'})
export class ClipboardSnackService {
  constructor(
    private readonly snackNotificationService: SnackNotificationService,
    private readonly clipboard: Clipboard
  ) {
  }

  copyWithNotification(copyText: string, message?: string) {
    if (this.clipboard.copy(copyText)) {
      this.snackNotificationService.push(message || 'Copié dans le presse-papier', 'info');
    }
  }
}
