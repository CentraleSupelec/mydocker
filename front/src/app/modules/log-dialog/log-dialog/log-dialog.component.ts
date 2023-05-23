import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

@Component({
  templateUrl: './log-dialog.component.html',
  styleUrls: ['./log-dialog.component.css']
})
export class LogDialogComponent {

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: string,
    private readonly dialogRef: MatDialogRef<LogDialogComponent>,
  ) {}

  close() {
    this.dialogRef.close();
  }
}
