import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TruncatePipe } from './truncate.pipe';
import { GenerateJoinLinkPipe } from "./generate-join-link.pipe";
import { GenerateMagicLinkPipe } from './generate-magic-link.pipe';
import { RemoveMarkdownPipe } from './remove-markdown.pipe';
import { LexicalEditorComponent } from './lexical/lexical-editor/lexical-editor.component';
import { LexicalLinkPopupComponent } from './lexical/lexical-link-popup/lexical-link-popup.component';
import { LexicalImagePopupComponent } from './lexical/lexical-image-popup/lexical-image-popup.component';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';



@NgModule({
  declarations: [
    TruncatePipe,
    GenerateJoinLinkPipe,
    GenerateMagicLinkPipe,
    RemoveMarkdownPipe,
    LexicalEditorComponent,
    LexicalLinkPopupComponent,
    LexicalImagePopupComponent,
  ],
  exports: [
    TruncatePipe,
    GenerateJoinLinkPipe,
    GenerateMagicLinkPipe,
    RemoveMarkdownPipe,
    LexicalEditorComponent,
    LexicalLinkPopupComponent,
    LexicalImagePopupComponent
  ],
  imports: [
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatMenuModule,
    MatIconModule,
    MatDialogModule,
    CommonModule
  ]
})
export class UtilsModule { }
