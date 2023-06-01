import { Component, Inject } from '@angular/core';
import { MAT_SNACK_BAR_DATA } from '@angular/material/snack-bar';
import { ISnackData } from '../toast';

@Component({
  templateUrl: './snack-notification.component.html',
  styleUrls: ['./snack-notification.component.css']
})
export class SnackNotificationComponent {

  constructor(
    @Inject(MAT_SNACK_BAR_DATA) public data: ISnackData,
  ) { }
}
