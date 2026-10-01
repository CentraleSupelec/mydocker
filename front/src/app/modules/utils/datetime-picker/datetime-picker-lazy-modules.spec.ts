import {
  ApplicationRef,
  Component,
  ComponentRef,
  createComponent,
  createNgModule,
  Injector,
  LOCALE_ID,
  NgModuleRef,
  Type
} from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DateAdapter, MAT_DATE_LOCALE } from '@angular/material/core';
import { DateFnsAdapter, MatDateFnsModule } from '@angular/material-date-fns-adapter';
import { fr } from 'date-fns/locale';
import { NgxPermissionsModule } from 'ngx-permissions';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { DatetimePickerModule } from './datetime-picker.module';
import { AdminCourseModule } from '../../admin-course/admin-course.module';
import { DeploymentModule } from '../../deployment/deployment.module';
import { SessionsResourcesModule } from '../../sessions-resources/sessions-resources.module';

/**
 * Typed dates through the app-datetime-picker, rendered inside the injector of each lazy route
 * module, created the way the router loads it under the root AppModule date providers.
 */
@Component({
  standalone: true,
  imports: [ReactiveFormsModule, DatetimePickerModule],
  template: `
    <form [formGroup]="form">
      <input formControlName="title">
      <app-datetime-picker formControlName="date"></app-datetime-picker>
    </form>
  `,
})
class DateFormHostComponent {
  readonly form = new FormGroup({
    title: new FormControl('Cours', Validators.required),
    date: new FormControl<number | null>(null, Validators.required),
  });
}

const lazyModules: [string, Type<unknown>][] = [
  ['AdminCourseModule', AdminCourseModule],
  ['DeploymentModule', DeploymentModule],
  ['SessionsResourcesModule', SessionsResourcesModule],
];

lazyModules.forEach(([name, lazyModule]) => {
  describe(`Typed dates in lazy ${name}`, () => {
    let moduleRef: NgModuleRef<unknown>;
    let hostRef: ComponentRef<DateFormHostComponent>;
    let appRef: ApplicationRef;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
          RouterTestingModule,
          HttpClientTestingModule,
          NgxPermissionsModule.forRoot(),
          TranslateTestingModule,
          // Same date wiring as AppModule.
          MatDateFnsModule,
        ],
        providers: [
          { provide: LOCALE_ID, useValue: 'fr' },
          { provide: MAT_DATE_LOCALE, useValue: fr },
        ],
      });
      appRef = TestBed.inject(ApplicationRef);
      moduleRef = createNgModule(lazyModule, TestBed.inject(Injector));
      hostRef = createComponent(DateFormHostComponent, { environmentInjector: moduleRef.injector });
      document.body.appendChild(hostRef.location.nativeElement);
      appRef.attachView(hostRef.hostView);
      appRef.tick();
    });

    afterEach(() => {
      hostRef.destroy();
      hostRef.location.nativeElement.remove();
      moduleRef.destroy();
    });

    function dateInput(): HTMLInputElement {
      return hostRef.location.nativeElement.querySelector('app-datetime-picker input:not([type="time"])');
    }

    function timeInput(): HTMLInputElement {
      return hostRef.location.nativeElement.querySelector('app-datetime-picker input[type="time"]');
    }

    function type(input: HTMLInputElement, text: string): void {
      input.value = text;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('blur'));
      appRef.tick();
    }

    async function settle(): Promise<void> {
      appRef.tick();
      await new Promise(resolve => setTimeout(resolve));
      appRef.tick();
    }

    it('uses the date-fns adapter with the French locale', () => {
      const adapter = moduleRef.injector.get(DateAdapter);

      expect(adapter).toEqual(jasmine.any(DateFnsAdapter));
      expect(adapter.format(new Date(2026, 8, 3), 'PPP')).toBe('3 septembre 2026');
    });

    it('reads a typed date with a day above 12 as day/month/year', () => {
      type(dateInput(), '23/09/2026');

      expect(hostRef.instance.form.value.date).toBe(new Date(2026, 8, 23).getTime());
      expect(dateInput().value).toBe('23/09/2026');
    });

    it('reads 03/09/2026 as 3 September, not 9 March', () => {
      type(dateInput(), '03/09/2026');

      expect(hostRef.instance.form.value.date).toBe(new Date(2026, 8, 3).getTime());
      expect(dateInput().value).toBe('03/09/2026');
    });

    it('makes an otherwise valid form valid once a valid date is typed', () => {
      expect(hostRef.instance.form.valid).toBeFalse();

      type(dateInput(), '23/09/2026');

      expect(hostRef.instance.form.valid).toBeTrue();
    });

    it('keeps an impossible calendar date invalid', () => {
      type(dateInput(), '31/02/2026');

      expect(hostRef.instance.form.value.date).toBeNull();
      expect(hostRef.instance.form.valid).toBeFalse();
    });

    it('keeps the selected time when a date is typed', () => {
      type(timeInput(), '14:30');
      type(dateInput(), '03/09/2026');

      expect(hostRef.instance.form.value.date).toBe(new Date(2026, 8, 3, 14, 30).getTime());
    });

    it('gives the same local date and time from the calendar as from the keyboard', async () => {
      hostRef.instance.form.controls.date.setValue(new Date(2026, 8, 20, 14, 30).getTime());
      appRef.tick();
      hostRef.location.nativeElement.querySelector('mat-datepicker-toggle button').click();
      await settle();
      const cell = Array.from(document.querySelectorAll<HTMLElement>('.mat-calendar-body-cell'))
        .find(c => c.textContent?.trim() === '3');
      expect(cell).withContext('calendar cell for the 3rd').toBeDefined();
      cell?.click();
      await settle();
      const picked = hostRef.instance.form.value.date;

      expect(picked).toBe(new Date(2026, 8, 3, 14, 30).getTime());

      type(dateInput(), '03/09/2026');

      expect(hostRef.instance.form.value.date).toBe(picked);
      expect(dateInput().value).toBe('03/09/2026');
    });
  });
});
