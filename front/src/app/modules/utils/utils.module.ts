import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TruncatePipe } from './truncate.pipe';
import { GenerateJoinLinkPipe } from "./generate-join-link.pipe";
import { GenerateMagicLinkPipe } from './generate-magic-link.pipe';



@NgModule({
  declarations: [
    TruncatePipe,
    GenerateJoinLinkPipe,
    GenerateMagicLinkPipe
  ],
  exports: [
    TruncatePipe,
    GenerateJoinLinkPipe,
    GenerateMagicLinkPipe
  ],
  imports: [
    CommonModule
  ]
})
export class UtilsModule { }
