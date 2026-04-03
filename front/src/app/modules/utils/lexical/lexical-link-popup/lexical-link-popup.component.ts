import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

@Component({
    selector: 'app-lexical-link-popup',
    templateUrl: './lexical-link-popup.component.html',
    styleUrls: ['./lexical-link-popup.component.css']
  })
  export class LexicalLinkPopupComponent {
  
    constructor(
      public dialogRef: MatDialogRef<LexicalLinkPopupComponent>,
      @Inject(MAT_DIALOG_DATA) public link: string) {}
  
    onCancel(): void {
      this.dialogRef.close();
    } 
}
