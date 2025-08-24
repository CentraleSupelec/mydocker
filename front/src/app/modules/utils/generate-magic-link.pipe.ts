import { Inject, Pipe, PipeTransform } from "@angular/core";
import { APP_CONFIG, IAppConfig } from "../../app-config";

@Pipe({
  name: 'generateMagicLink'
})
export class GenerateMagicLinkPipe implements PipeTransform {

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,) {
  }

  transform(value: string| undefined): string {
    if(!value) {
      return '';
    }
    return `${ this.config.front_url}/course/${ value }/magic-link`;
  }
}
