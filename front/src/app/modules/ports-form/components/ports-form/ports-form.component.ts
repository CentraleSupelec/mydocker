import { ChangeDetectorRef, Component, forwardRef, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator, Validators
} from "@angular/forms";
import { IPort } from "../../interfaces/port";

@Component({
  selector: 'app-ports-form',
  templateUrl: './ports-form.component.html',
  styleUrls: ['./ports-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PortsFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => PortsFormComponent),
      multi: true,
    },
  ]
})
export class PortsFormComponent implements OnInit, ControlValueAccessor, Validator {
  readonly portsFormArray: FormArray;

  private propagateChange = (_: IPort[]) => {};

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly cd: ChangeDetectorRef,
  ) {
    this.portsFormArray = formBuilder.array([], Validators.required);
  }

  ngOnInit(): void {
    this.portsFormArray.valueChanges
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
    if (this.portsFormArray.invalid) {
      return { ports: this.portsFormArray.errors }
    }
    return null;
  }

  writeValue(obj: IPort[]): void {
    // remove controls
    for (let i = this.portsFormArray.length - 1; i >= 0; i--) {
      this.portsFormArray.removeAt(i);
    }
    // set new value
    for (let i = 0; i < obj.length; i++) {
      this.portsFormArray.setControl(
        i,
        this.formBuilder.control(obj[i]),
      );
    }
  }

  addPort() {
    this.portsFormArray.push(
      this.formBuilder.control({})
    );
    this.cd.detectChanges();
    this.portsFormArray.updateValueAndValidity();
  }

  remove(index: number) {
    this.portsFormArray.removeAt(index);
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.portsFormArray.disable();
    } else {
      this.portsFormArray.enable();
    }
  }
}
