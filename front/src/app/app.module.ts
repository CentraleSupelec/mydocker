import { LOCALE_ID, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import { RouterModule } from "@angular/router";
import { routes } from "./app.routes";
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { APP_CONFIG, appConstantFactory } from "./app-config";
import { AuthenticationModule } from "./modules/authentication/authentication.module";
import { UserInterfaceModule } from "./modules/user-interface/user-interface.module";
import { NgxPermissionsModule } from "ngx-permissions";
import { FlexLayoutModule } from "@angular/flex-layout";
import { SnackNotificationModule } from "./modules/utils/snack-notification/snack-notification.module";
import { ProgressBarModule } from "./modules/utils/progress-bar/progress-bar.module";
import { MonacoEditorModule, NgxMonacoEditorConfig } from "ngx-monaco-editor-v2";
import { registerLocaleData } from "@angular/common";
import { MatDateFnsModule } from "@angular/material-date-fns-adapter";
import localeFr from '@angular/common/locales/fr';
import localeEn from '@angular/common/locales/en';
import { MAT_DATE_LOCALE } from "@angular/material/core";
import { fr } from "date-fns/locale";
import { TranslateCompiler, TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { TranslateMessageFormatCompiler } from 'ngx-translate-messageformat-compiler';


const monacoConfig: NgxMonacoEditorConfig = {
  baseUrl: 'assets',
};

registerLocaleData(localeFr, 'fr');
registerLocaleData(localeEn, 'en');

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    RouterModule.forRoot(routes, {
      paramsInheritanceStrategy: 'always',
      onSameUrlNavigation: 'reload',
    }),
    BrowserAnimationsModule,
    AuthenticationModule,
    UserInterfaceModule,
    NgxPermissionsModule.forRoot(),
    FlexLayoutModule,
    SnackNotificationModule,
    ProgressBarModule,
    MonacoEditorModule.forRoot(monacoConfig),
    MatDateFnsModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [HttpClient],
      },
      compiler: {
        provide: TranslateCompiler,
        useClass: TranslateMessageFormatCompiler
      }
    }),
  ],
  providers: [
    {
      provide: APP_CONFIG,
      useFactory: appConstantFactory
    },
    { provide: LOCALE_ID, useValue: 'fr' },
    { provide: MAT_DATE_LOCALE, useValue: fr }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
