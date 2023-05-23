import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup, NG_VALIDATORS, NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { CourseStatus } from "../../interfaces/course";
import { ISessionsById } from "../../../sessions-form/interfaces/admin-session";

@Component({
  selector: 'app-course-general-information-form',
  templateUrl: './course-general-information-form.component.html',
  styleUrls: ['./course-general-information-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CourseGeneralInformationFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CourseGeneralInformationFormComponent),
      multi: true,
    },
  ]
})
export class CourseGeneralInformationFormComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {
  @Input() sessionsById?: ISessionsById;
  readonly formGroup: FormGroup;
  private destroy$ = new Subject<void>();
  readonly courseStatus = Object.entries(CourseStatus);

  constructor(
    formBuilder: FormBuilder
  ) {
    this.formGroup = formBuilder.group({
      title: ['', [Validators.required, Validators.pattern('^[^/]+$')]],
      description: ['', Validators.required],
      sessions: [[]],
      automaticShutdown: [false],
      shutdownAfterHours: [],
      shutdownAfterMinutesRemainder: [],
      warnShutdownHours: [],
      warnShutdownMinutesRemainder: [],
      status: CourseStatus.DRAFT,
    }, {
      validators: CourseGeneralInformationFormComponent.validateShutdownSettings
    });
  }

  private static computeTotalDuration(hours?: number | '', minutes?: number | ''): number {
    return (hours || 0) * 60 + (minutes || 0);
  }

  private static validateShutdownSettings(control: AbstractControl): ValidationErrors | null{
    const payload = control.value;
    const errors = {} as ValidationErrors;
    if (!payload?.automaticShutdown) {
      return null;
    }
    const shutdownAfterMinutes = CourseGeneralInformationFormComponent.computeTotalDuration(payload?.shutdownAfterHours, payload?.shutdownAfterMinutesRemainder);
    const warnShutdownMinutes = CourseGeneralInformationFormComponent.computeTotalDuration(payload?.warnShutdownHours, payload?.warnShutdownMinutesRemainder);
    if (!shutdownAfterMinutes) {
      errors['missingShutdownAfterMinutes'] = true;
    }
    if (shutdownAfterMinutes <= warnShutdownMinutes) {
      errors['warnAfterShutdown'] = true;
    }
    return Object.keys(errors).length > 0 ? errors : null;
  }

  ngOnInit(): void {
    this.formGroup.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
        ({
           shutdownAfterMinutesRemainder,
           shutdownAfterHours,
           warnShutdownMinutesRemainder,
           warnShutdownHours,
           automaticShutdown,
           ...v
         }) => {
          return this.propagateChange({
            shutdownAfterMinutes: automaticShutdown ? CourseGeneralInformationFormComponent.computeTotalDuration(shutdownAfterHours, shutdownAfterMinutesRemainder) : 0,
            warnShutdownMinutes: automaticShutdown ? CourseGeneralInformationFormComponent.computeTotalDuration(warnShutdownHours, warnShutdownMinutesRemainder) : 0,
            ...v,
          });
        }
      )
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
      this.formGroup.disable()
    } else {
      this.formGroup.enable()
    }
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.formGroup.invalid) {
      return {courseGeneralInformationErrors: this.formGroup.errors}
    }
    return null;
  }

  writeValue(obj: any): void {
    this.formGroup.setValue({
      title: obj?.title || '',
      description: obj?.description || '',
      sessions: obj?.sessions || [],
      status: obj?.status || CourseStatus.DRAFT,
      automaticShutdown: obj?.shutdownAfterMinutes > 0,
      shutdownAfterHours: obj?.shutdownAfterMinutes ? Math.floor(obj?.shutdownAfterMinutes / 60) : '',
      shutdownAfterMinutesRemainder: obj?.shutdownAfterMinutes ? obj?.shutdownAfterMinutes % 60 : '',
      warnShutdownHours: obj?.warnShutdownMinutes ? Math.floor(obj?.warnShutdownMinutes / 60) : '',
      warnShutdownMinutesRemainder: obj?.warnShutdownMinutes ? obj?.warnShutdownMinutes % 60 : '',
    });
  }

  private propagateChange = (_: any) => {}
}
