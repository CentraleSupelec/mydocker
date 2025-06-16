import { Injectable } from "@angular/core";
import { IContainerPort } from "../shell/interfaces/container-port";
import { ConnectionType } from "../ports-form/interfaces/port";

class WrongPortType extends Error {
  message = 'HOST est uniquement pour les ports de type HTTP';
}

@Injectable({
  providedIn: 'root'
})
export class RenderStringService {

  private readonly regex = /{{([^{}]+)}}/g;
  private readonly portRegex = /PORT\['([0-9]+)']/;
  private readonly hostRegex = /HOST\['([0-9]+)']/;

  private readonly defaultValueEnabled = new Set<string>([
    'USER_REDIRECT',
    'USER-REDIRECT',
  ]);

  renderString(stringToRender: string, ports: IContainerPort[], username: string, password: string, ip: string, userRedirect: string) {
    try {
      let returnHostname = false;
      const replaced = stringToRender.replace(this.regex, (_: string, group: string) => {
        const [placeholder, defaultValue] = group.split(":", 2);
        let replacement = '';

        switch (true) {
          case placeholder === 'IP':
            replacement = ip;
            break;
          case placeholder === 'USERNAME':
            replacement = username;
            break;
          case placeholder === 'PASSWORD':
            replacement = password;
            break;
          case placeholder === 'USER_REDIRECT':
          case placeholder === 'USER-REDIRECT':
            replacement = userRedirect ? decodeURI(userRedirect) : '';
            break;
          case this.portRegex.test(placeholder): {
            const matchPort = placeholder.match(this.portRegex)!;
            const matchingContainerPort = ports.find(p => String(p.mapPort) === matchPort[1]);
            replacement = matchingContainerPort ? String(matchingContainerPort.portMapTo) : '';
            break;
          }
          case this.hostRegex.test(placeholder): {
            const matchHost = placeholder.match(this.hostRegex)!;
            const matchingContainerPort = ports.find(p => String(p.mapPort) === matchHost[1]);
            if (matchingContainerPort?.connectionType !== ConnectionType.HTTP) {
              throw new WrongPortType();
            }
            returnHostname = true;
            replacement = matchingContainerPort?.hostname ?? 'generated-hostname.mydocker.com';
            break;
          }
          default:
            replacement = '';
        }

        if (replacement === '' && defaultValue !== undefined && this.defaultValueEnabled.has(placeholder)) {
          replacement = defaultValue;
        }

        return replacement;
      });
      return returnHostname ? replaced.replace(/^http:\/\//, 'https://') : replaced;
    } catch (error) {
      console.error(error);
      if (error instanceof WrongPortType) {
        return error.message;
      }
      return '';
    }
  }
}
