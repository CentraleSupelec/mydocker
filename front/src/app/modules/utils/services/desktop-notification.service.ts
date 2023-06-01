import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DesktopNotificationService {
  private audio?: HTMLAudioElement;

  constructor() {
    this.audio = new Audio('../../../assets/sfx-pop3.mp3');
    this.audio.load();
  }

  async askPermissions(): Promise<NotificationPermission | void> {
    if (!('Notification' in window)) {
      return;
    }
    if (this.isAllowed()) {
      return window.Notification.permission;
    }
    return await window.Notification.requestPermission();
  }

  isAllowed(): boolean {
    return window.Notification && window.Notification.permission === 'granted';
  }

  async notify(title: string, message: string): Promise<Notification | void> {
    await this.audio?.play();
    if (this.isAllowed()) {
      const notification = new window.Notification(title, {body: message, requireInteraction: true});
      notification.onclick = function () {
        window.focus();
        notification.close();
      }
    } else {
      window.setTimeout(() => {
        window.alert(`${title} / ${message}`);
      }, 10);
    }
  }
}
