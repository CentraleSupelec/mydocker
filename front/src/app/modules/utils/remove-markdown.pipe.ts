import { Pipe, PipeTransform } from '@angular/core';
import removeMd from 'remove-markdown';

@Pipe({
  name: 'removeMarkdown'
})
export class RemoveMarkdownPipe implements PipeTransform {

  transform(value: string): string {
    return removeMd(value);
  }
}
