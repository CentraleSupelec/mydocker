import { Component, Inject } from "@angular/core";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

export interface ImageDialogData {
  link: string;
  alt: string;
}

@Component({
    selector: 'app-lexical-image-popup',
    templateUrl: './lexical-image-popup.component.html',
    styleUrls: ['./lexical-image-popup.component.css']
  })
  export class LexicalImagePopupComponent {
  
    constructor(
      public dialogRef: MatDialogRef<LexicalImagePopupComponent>,
      @Inject(MAT_DIALOG_DATA) public data: ImageDialogData,
    ) {}
  
    onCancel(): void {
      this.dialogRef.close();
    } 
}
