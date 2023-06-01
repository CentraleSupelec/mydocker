import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup, NG_VALIDATORS, NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from "@angular/forms";
import { IResourceDescription } from "../../../sessions-resources/interfaces/session-with-resources";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";

@Component({
  selector: 'app-resource-form',
  templateUrl: './resource-form.component.html',
  styleUrls: ['./resource-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ResourceFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ResourceFormComponent),
      multi: true,
    },
  ]
})
export class ResourceFormComponent implements OnInit, ControlValueAccessor, Validator {
  readonly sessionFormGroup: FormGroup;
  @Input() resources: IOvhResource[] = [];

  private propagateChange = (_: IResourceDescription) => {};
  constructor(
    formBuilder: FormBuilder,
  ) {
    this.sessionFormGroup = formBuilder.group({
      ovhResourceId: [null, Validators.required],
      count: [null, Validators.required],
    })
  }

  ngOnInit(): void {
    this.sessionFormGroup.valueChanges
      .subscribe(
        v => this.propagateChange({
          count: v.count,
          ovhResourceId: parseInt(v.ovhResourceId, 10)
        })
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

  writeValue(obj: IResourceDescription): void {
    this.sessionFormGroup.setValue({
      ovhResourceId: String(obj?.ovhResourceId) || null,
      count: obj?.count || null,
    });
  }
}
