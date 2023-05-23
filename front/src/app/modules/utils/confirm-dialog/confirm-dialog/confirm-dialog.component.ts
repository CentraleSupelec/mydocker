import { Component } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { IDialogMessage } from '../dialog';

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.scss'],
})
export class ConfirmDialogComponent {

  public dialogMessage: IDialogMessage | undefined;

  constructor(public dialogRef: MatDialogRef<ConfirmDialogComponent>) {
  }
}
