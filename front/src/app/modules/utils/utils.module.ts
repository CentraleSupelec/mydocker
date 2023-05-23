import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TruncatePipe } from './truncate.pipe';
import { GenerateJoinLinkPipe } from "./generate-join-link.pipe";



@NgModule({
  declarations: [
    TruncatePipe,
    GenerateJoinLinkPipe
  ],
  exports: [
    TruncatePipe,
    GenerateJoinLinkPipe
  ],
  imports: [
    CommonModule
  ]
})
export class UtilsModule { }
