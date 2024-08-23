import { APP_INITIALIZER, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SignInComponent } from "./components/sign-in/sign-in.component";
import { MatButtonModule } from "@angular/material/button";
import { AuthGuard } from "./services/auth.guard";
import { NgxPermissionsModule } from "ngx-permissions";
import { HTTP_INTERCEPTORS, HttpClientModule } from "@angular/common/http";
import { LoginAcceptComponent } from './components/login-accept/login-accept.component';
import { TokenService } from "./services/token.service";
import { AddTokenInterceptor } from "./services/add-token.interceptor";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { AutologinGuard } from './services/autologin.guard';
import { UtilsModule } from "../utils/utils.module";
import { AuthModule, LogLevel, StsConfigLoader, StsConfigStaticLoader } from "angular-auth-oidc-client";
import { APP_CONFIG, IAppConfig } from "../../app-config";

const loadPermissionsOnStartupAppInitializerFactory = (tokenService: TokenService) =>
  function(): Promise<any> {
    return tokenService.signInFromStorage()
      .toPromise();
  };

const generateAuthConfiguration = (appConfig: IAppConfig) => {
  return new StsConfigStaticLoader({
    authority: appConfig.oidc_authority,
    redirectUrl: `${window.location.origin}/loginAccept`,
    postLogoutRedirectUri: `${window.location.origin}/login?preventAutoSignIn=true`,
    clientId: appConfig.oidc_client_id,
    scope: appConfig.oidc_scope,
    responseType: 'code',
    silentRenew: false,
    logLevel: LogLevel.Warn,
    triggerAuthorizationResultEvent: true,
  });
}


@NgModule({
  declarations: [
    SignInComponent,
    LoginAcceptComponent,
  ],
  imports: [
    CommonModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    NgxPermissionsModule,
    HttpClientModule,
    UtilsModule,
    AuthModule.forRoot({
      loader: {
        provide: StsConfigLoader,
        useFactory: generateAuthConfiguration,
        deps: [APP_CONFIG],
      }
    }),
  ],
  providers: [
    AuthGuard,
    AutologinGuard,
    {
      provide: APP_INITIALIZER,
      useFactory: loadPermissionsOnStartupAppInitializerFactory,
      deps: [TokenService],
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AddTokenInterceptor,
      multi: true,
    },
  ]
})
export class AuthenticationModule {}
