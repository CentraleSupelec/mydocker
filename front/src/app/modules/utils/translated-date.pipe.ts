import { ChangeDetectorRef, Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

@Pipe({
  name: 'translatedDate',
  pure: false
})
export class TranslatedDatePipe implements PipeTransform {
  private langSub: Subscription;

  constructor(
    private datePipe: DatePipe,
    private translate: TranslateService,
    private cdr: ChangeDetectorRef
  ) {
    this.langSub = this.translate.onLangChange.subscribe(() => {
      this.cdr.markForCheck();
    });
  }

  transform(
    dateTime: any,
    dateFormat: string|null = 'dd/MM/yy',
    timeFormat: string|null = 'HH:mm',
    omitOn: boolean = false
  ): string {
    if (!dateTime) return '';
    let timeString: string | null = '';
    let dateString: string | null = '';

    if (dateFormat) {
      const on = this.translate.instant('admin.resources_management.deployment.edit.on');
      const date = this.datePipe.transform(dateTime, dateFormat, '', this.translate.currentLang);
      dateString = `${omitOn? '': on + ' '}${date}`;
    }

    if (timeFormat) {
      const at = this.translate.instant('admin.resources_management.deployment.edit.at');
      const time = this.datePipe.transform(dateTime, timeFormat, '', this.translate.currentLang);
      timeString = `${at} ${time}`;
    }

    const separator = `${dateFormat ? ' ': ''}`;

    return `${dateString}${separator}${timeString}`;
  }

  ngOnDestroy() {
    this.langSub?.unsubscribe();
  }
}
