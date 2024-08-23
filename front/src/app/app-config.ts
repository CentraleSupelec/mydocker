import { InjectionToken } from "@angular/core";
import { TokenOrigin } from "./modules/authentication/interfaces/jwt-token";


export interface IAppConfig {
  back_url: string;
  front_url: string;
  cas: {
    login_url: string;
    logout_url: string;
  };
  default_storage_backend?: string;
  deployment_enabled: boolean;
  auto_login?: TokenOrigin;
  oidc_default_idp?: string;
  oidc_client_id?: string;
  oidc_authority?: string;
  oidc_scope?: string;
}

export const APP_CONFIG = new InjectionToken<IAppConfig>('config');

export function appConstantFactory(): IAppConfig {
  // @ts-ignore
  const appConstants: IAppConfig = window['MY_DOCKER_APP_CONSTANTS'] || {};
  return {
    back_url: appConstants.back_url,
    front_url: appConstants.front_url,
    cas: {
      login_url: appConstants.cas?.login_url,
      logout_url: appConstants.cas?.logout_url,
    },
    default_storage_backend: appConstants.default_storage_backend,
    deployment_enabled: appConstants.deployment_enabled,
    auto_login: appConstants.auto_login,
    oidc_default_idp: appConstants.oidc_default_idp,
    oidc_client_id: appConstants.oidc_client_id,
    oidc_authority: appConstants.oidc_authority,
    oidc_scope: appConstants.oidc_scope,
  };
}
