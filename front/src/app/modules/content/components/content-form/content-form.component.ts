import { Component, forwardRef, Inject, OnDestroy, OnInit } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR, ValidationErrors,
  Validator, ValidatorFn, Validators
} from '@angular/forms';
import { IContent } from '../../interfaces/content';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-content-form',
  templateUrl: './content-form.component.html',
  styleUrls: ['./content-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ContentFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ContentFormComponent),
      multi: true,
    },
  ]
})
export class ContentFormComponent implements OnInit, ControlValueAccessor, OnDestroy, Validator {
  contentForm: FormGroup;
  private propagateChange = (_: IContent) => {};
  private destroy$: Subject<void> = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    @Inject(APP_CONFIG) readonly config: IAppConfig
  ) {

    this.contentForm = fb.group({
      title: ['', Validators.required],
      slug: [{value: '', disabled: true}],
      enabled: [false, Validators.required],
      richText: ['', [Validators.required, this.notEmptyHtmlValidator()]],
    });
  }

  ngOnInit(): void {
    this.contentForm.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.propagateChange(this.contentForm.getRawValue())
      })
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.contentForm.invalid) {
      return {
        contentErrors: this.contentForm.errors
      };
    }
    return null;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  setDisabledState(isDisabled: boolean) {
    if (isDisabled) {
      this.contentForm.disable({ emitEvent: false });
    } else {
      this.contentForm.enable();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
  }

  writeValue(obj?: IContent): void {
    this.contentForm.setValue({
      title: obj?.title || '',
      slug: obj?.slug || '',
      enabled: obj?.enabled || false,
      richText: obj?.richText || '',
    })
  }

  hasRequiredValidator(controlName: string): boolean {
    const control = this.contentForm.get(controlName);
    if (!control || !control.validator) return false;
    return control.hasValidator(Validators.required);
  }

  private notEmptyHtmlValidator(): ValidatorFn {
    return (control: AbstractControl) => {
      const value = control.value ?? '';
      const stripped = value.replace(/<[^>]*>/g, '').trim();
      return stripped ? null : { emptyHtml: true };
    };
  }
}
