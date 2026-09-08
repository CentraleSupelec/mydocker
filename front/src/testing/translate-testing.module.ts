import { NgModule } from '@angular/core';
import { TranslateCompiler, TranslateModule, TranslateService } from '@ngx-translate/core';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';

import fr from 'src/assets/i18n/fr.json';

/**
 * Translation support for specs.
 *
 * The application configures TranslateModule.forRoot with an HTTP loader and the messageformat
 * compiler. A spec needs no loader: without one the pipe echoes the key, which is enough for a
 * template to compile and render. What a spec does need is the root providers, since a component
 * template using the translate pipe fails to instantiate without them.
 */
@NgModule({
  imports: [TranslateModule.forRoot()],
  exports: [TranslateModule],
})
export class TranslateTestingModule {}

/**
 * Translation support for the few specs that assert rendered French copy rather than structure.
 *
 * Those need the real fr.json and the same messageformat compiler as the application, because the
 * catalogue interpolates with {param} rather than the ngx-translate default {{param}}.
 */
@NgModule({
  imports: [
    TranslateModule.forRoot({
      compiler: { provide: TranslateCompiler, useClass: TranslateMessageFormatCompiler },
    }),
  ],
  exports: [TranslateModule],
})
export class TranslateFrenchTestingModule {
  constructor(translate: TranslateService) {
    translate.setTranslation('fr', fr);
    translate.use('fr');
  }
}
