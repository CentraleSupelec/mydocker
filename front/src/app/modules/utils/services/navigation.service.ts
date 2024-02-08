import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  navigateTo(string: string): void {
    window.location.href = string;
  }
}
