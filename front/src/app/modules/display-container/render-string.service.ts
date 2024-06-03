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

  private readonly regex = /{{([a-zA-Z\[\]'0-9]+)}}/g;
  private readonly portRegex = /PORT\['([0-9]+)']/;
  private readonly hostRegex = /HOST\['([0-9]+)']/;

  renderString(stringToRender: string, ports: IContainerPort[], username: string, password: string, ip: string) {
    try {
      let returnHostname = false;
      const replaced = stringToRender.replace(this.regex, (_: string, group: string) => {
        switch (group) {
          case 'IP':
            return ip
          case 'USERNAME':
            return username;
          case 'PASSWORD':
            return password
        }
        const matchPort = group.match(this.portRegex);
        if (matchPort !== null) {
          const matchingContainerPort = ports.find(p => String(p.mapPort) === matchPort[1])
          return matchingContainerPort ? String(matchingContainerPort.portMapTo) : ''
        }
        const matchHost = group.match(this.hostRegex);
        if (matchHost !== null) {
          const matchingContainerPort = ports.find(p => String(p.mapPort) === matchHost[1])
          if (matchingContainerPort?.connectionType !== ConnectionType.HTTP) {
            throw new WrongPortType();
          }
          returnHostname = true;
          return matchingContainerPort.hostname ?? 'generated-hostname.mydocker.com';
        }
        return '';
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
