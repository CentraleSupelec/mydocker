import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { IServiceLogs } from "../../shell/interfaces/container";
import { stripRegistryHost } from "../image-reference";

@Component({
  templateUrl: './log-dialog.component.html',
  styleUrls: ['./log-dialog.component.css']
})
export class LogDialogComponent {

  readonly imageReference: string;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: IServiceLogs,
    private readonly dialogRef: MatDialogRef<LogDialogComponent>,
  ) {
    this.imageReference = stripRegistryHost(this.data?.image);
  }

  close() {
    this.dialogRef.close();
  }
}
