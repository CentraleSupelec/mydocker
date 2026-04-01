import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TruncatePipe } from './truncate.pipe';
import { GenerateJoinLinkPipe } from "./generate-join-link.pipe";
import { GenerateMagicLinkPipe } from './generate-magic-link.pipe';
import { RemoveMarkdownPipe } from './remove-markdown.pipe';



@NgModule({
  declarations: [
    TruncatePipe,
    GenerateJoinLinkPipe,
    GenerateMagicLinkPipe,
    RemoveMarkdownPipe
  ],
  exports: [
    TruncatePipe,
    GenerateJoinLinkPipe,
    GenerateMagicLinkPipe,
    RemoveMarkdownPipe
  ],
  imports: [
    CommonModule
  ]
})
export class UtilsModule { }
