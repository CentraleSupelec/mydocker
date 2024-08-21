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
import { AuthModule, LogLevel } from 'angular-auth-oidc-client';


export const loadPermissionsOnStartupAppInitializerFactory = (tokenService: TokenService) =>
  function(): Promise<any> {
    return tokenService.signInFromStorage()
      .toPromise();
  };


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
      config: {
        // Todo : Mettre en config
        authority: 'https://keycloak.centralesupelec.fr/realms/mydocker-cs-preprod',
        redirectUrl: `${window.location.origin}/loginAccept`,
        postLogoutRedirectUri: window.location.origin,
        // Todo : Mettre en config
        clientId: 'mydocker-local',
        scope: 'openid profile email edu',
        responseType: 'code',
        silentRenew: true,
        useRefreshToken: true,
        logLevel: LogLevel.Debug,
      },
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
