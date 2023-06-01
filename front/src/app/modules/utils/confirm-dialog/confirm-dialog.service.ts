import { Injectable } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { IDialogMessage } from './dialog';
import { ConfirmDialogComponent } from './confirm-dialog/confirm-dialog.component';
import { Observable } from "rxjs";


@Injectable()
export class ConfirmDialogService {
  constructor(
    private readonly dialog: MatDialog,
  ) {

  }

  confirm(dialogMessage: IDialogMessage): Observable<boolean> {
    const dialogRef: MatDialogRef<ConfirmDialogComponent> = this.dialog.open(ConfirmDialogComponent);
    dialogRef.componentInstance.dialogMessage = dialogMessage;
    return dialogRef.afterClosed();
  }
}
