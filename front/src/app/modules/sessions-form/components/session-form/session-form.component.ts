import {Component, forwardRef, Input, OnInit} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup, NG_VALIDATORS, NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from "@angular/forms";
import { IAdminSession } from "../../interfaces/admin-session";

@Component({
  selector: 'app-session-form',
  templateUrl: './session-form.component.html',
  styleUrls: ['./session-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SessionFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SessionFormComponent),
      multi: true,
    },
  ]
})
export class SessionFormComponent implements OnInit, ControlValueAccessor, Validator {
  readonly sessionFormGroup: FormGroup;

  private propagateChange = (_: IAdminSession) => {};
  constructor(
    formBuilder: FormBuilder,
  ) {
    this.sessionFormGroup = formBuilder.group({
      id: null,
      startDateTime: [null, Validators.required],
      endDateTime: [null, Validators.required],
      blockContainerCreationBeforeStartTime: null,
      destroyContainerAfterEndTime: null,
      studentNumber: [null, Validators.required],
    })
  }

  ngOnInit(): void {
    this.sessionFormGroup.valueChanges
      .subscribe(
        v => this.propagateChange(v)
      );
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.sessionFormGroup.invalid) {
      return {session: this.sessionFormGroup.errors};
    }
    return null;
  }

  writeValue(obj: IAdminSession): void {
    this.sessionFormGroup.patchValue({
      id: obj?.id || null,
      startDateTime: obj?.startDateTime || null,
      endDateTime: obj?.endDateTime || null,
      blockContainerCreationBeforeStartTime: obj?.blockContainerCreationBeforeStartTime ?? false,
      destroyContainerAfterEndTime: obj?.destroyContainerAfterEndTime ?? false,
      studentNumber: obj?.studentNumber || null
    });
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.sessionFormGroup.disable();
    } else {
      this.sessionFormGroup.enable();
    }
  }
}
