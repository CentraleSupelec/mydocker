import { MatDialog } from "@angular/material/dialog";
import { LogDialogComponent } from "./log-dialog/log-dialog.component";
import { Injectable } from "@angular/core";

@Injectable()
export class OpenLogDialogService {

  constructor(
    private readonly dialog: MatDialog,
  ) { }

  public openDialog(logs: string) {
    this.dialog.open(
      LogDialogComponent, {
        data: logs,
        width: '60%'
      }
    )
  }
}
