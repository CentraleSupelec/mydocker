import { Injectable } from '@angular/core';
import { IContainerPort } from "../shell/interfaces/container-port";

@Injectable({
  providedIn: 'root'
})
export class RenderStringService {
  private readonly regex = /{{([a-zA-Z\[\]'0-9]+)}}/g;
  private readonly portRegex = /PORT\['([0-9]+)']/;

  renderString(stringToRender: string, ports: IContainerPort[], username: string, password: string, ip: string) {
    return stringToRender.replace(this.regex, (_:string, group: string) => {
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
      return ''
    });
  }
}
