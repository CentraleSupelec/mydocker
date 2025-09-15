import { Pipe, PipeTransform } from '@angular/core';
import removeMd from 'remove-markdown';

@Pipe({
  name: 'removeMarkdown'
})
export class RemoveMarkdownPipe implements PipeTransform {
  transform(value: string): string {
    const withLineBreaks = value
      .replace(/<\/?(h[1-6]|p|li|blockquote|div|ul|ol)>/gi, '\n')
      .replace(/<br\s*\/?>/gi, '\n');

    const cleanHtml = withLineBreaks.replace(/<[^>]+>/g, '');

    return removeMd(cleanHtml).replace(/\n\s*\n/gi, '\n');
  }
}
