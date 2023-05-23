import { Component, forwardRef, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup, NG_VALIDATORS, NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from "@angular/forms";
import { ConnectionType, IPort } from "../../interfaces/port";

@Component({
  selector: 'app-port-form',
  templateUrl: './port-form.component.html',
  styleUrls: ['./port-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PortFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PortFormComponent),
      multi: true,
    },
  ]
})
export class PortFormComponent implements OnInit, ControlValueAccessor, Validator {
  readonly portFormGroup: FormGroup;
  readonly connectionType = Object.entries(ConnectionType);

  private propagateChange = (_: IPort) => {};
  constructor(
    formBuilder: FormBuilder,
  ) {
    this.portFormGroup = formBuilder.group({
      description: ['', Validators.required],
      mapPort:[null, Validators.required],
      connectionType: ['', Validators.required],
      requiredToAccessContainer: false
    })
  }

  ngOnInit(): void {
    this.portFormGroup.valueChanges
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
    if (this.portFormGroup.invalid) {
      return {port: this.portFormGroup.errors};
    }
    return null;
  }

  writeValue(obj: IPort): void {
    this.portFormGroup.setValue({
      description: obj.description || '',
      mapPort: obj.mapPort || null,
      connectionType: obj.connectionType || '',
      requiredToAccessContainer: obj.requiredToAccessContainer || false,
    });
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.portFormGroup.disable();
    } else {
      this.portFormGroup.enable();
    }
  }
}
