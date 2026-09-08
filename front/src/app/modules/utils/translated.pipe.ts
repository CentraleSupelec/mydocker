import { Pipe, PipeTransform } from '@angular/core';
import { ITranslation, Language } from '../../app-config';

@Pipe({
  name: 'translated'
})
export class TranslatedPipe implements PipeTransform {

  transform(value: ITranslation | string | null | undefined, language: Language): string {
    if (!value) {
      return '';
    }
    // config.js is written per platform and is not validated against ITranslation: a platform
    // provisioned before the i18n migration still declares titles as plain strings, and one
    // translated by hand can be missing a language. Both used to render as an empty label.
    if (typeof value === 'string') {
      return value;
    }
    return value[language] || value[Language.FR] || value[Language.EN] || '';
  }
}
