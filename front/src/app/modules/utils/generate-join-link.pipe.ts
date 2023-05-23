import { Inject, Pipe, PipeTransform } from "@angular/core";
import { APP_CONFIG, IAppConfig } from "../../app-config";

@Pipe({
  name: 'generateJoinLink'
})
export class GenerateJoinLinkPipe implements PipeTransform {

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,) {
  }

  transform(value: string): string {
    if(!value) {
      return '';
    }
    return `${ this.config.front_url}/shell/join/${ value }`;
  }
}
