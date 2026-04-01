import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { formatDate } from "@angular/common";


@Component({
  selector: 'app-datetime-picker',
  templateUrl: './datetime-picker.component.html',
  styleUrls: ['./datetime-picker.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DatetimePickerComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DatetimePickerComponent),
      multi: true,
    },
  ],
})
export class DatetimePickerComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  @Input() datePlaceholder: string = "";
  @Input() timePlaceholder: string = "";

  readonly datetimeFormGroup: FormGroup;
  private destroy$: Subject<void> = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
    ) {
    this.datetimeFormGroup = formBuilder.group({
      date: [null, Validators.required],
      time: ['00:00', Validators.required],
    });
  }

  ngOnInit(): void {
    this.datetimeFormGroup.valueChanges
      .pipe(
        takeUntil(this.destroy$),
      )
      .subscribe(v => {
        const date = v.date;
        if (date) {
          date.setHours(...v.time.split(':'))
          this.propagateChange(date.getTime());
        } else {
          this.propagateChange(null);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.datetimeFormGroup.disable();
    } else {
      this.datetimeFormGroup.enable();
    }
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.datetimeFormGroup.invalid) {
      return {
        datetimeFormGroupInvalid: this.datetimeFormGroup,
      };
    } else {
      return null;
    }
  }

  writeValue(timestamp: number | null): void {
    if (timestamp) {
      const date = new Date(timestamp);
      this.datetimeFormGroup.setValue({
        date: date,
        time: formatDate(date, 'HH:mm','fr'),
      });
    } else {
      this.datetimeFormGroup.setValue({
        date: null,
        time: '00:00'
      });
    }
  }

  private propagateChange(_: number | null) {
  }
}
