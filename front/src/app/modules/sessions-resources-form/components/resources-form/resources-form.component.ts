import { ChangeDetectorRef, Component, forwardRef, Input, OnInit } from '@angular/core';
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
import { IResourceDescription } from "../../../sessions-resources/interfaces/session-with-resources";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";

@Component({
  selector: 'app-resources-form',
  templateUrl: './resources-form.component.html',
  styleUrls: ['./resources-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ResourcesFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ResourcesFormComponent),
      multi: true,
    },
  ]
})
export class ResourcesFormComponent implements OnInit, ControlValueAccessor, Validator {
  readonly sessionsFormArray: FormArray;
  @Input() resources: IOvhResource[] = [];

  private propagateChange = (_: IResourceDescription[]) => {};

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly cd: ChangeDetectorRef,
  ) {
    this.sessionsFormArray = formBuilder.array([], Validators.required);
  }

  ngOnInit(): void {
    this.sessionsFormArray.valueChanges
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
    if (this.sessionsFormArray.invalid) {
      return { ports: this.sessionsFormArray.errors }
    }
    return null;
  }

  writeValue(obj: IResourceDescription[]): void {
    // remove controls
    for (let i = this.sessionsFormArray.length - 1; i >= 0; i--) {
      this.sessionsFormArray.removeAt(i);
    }
    // set new value
    for (let i = 0; i < obj?.length; i++) {
      this.sessionsFormArray.setControl(
        i,
        this.formBuilder.control(obj[i]),
      );
    }
  }

  addResource() {
    this.sessionsFormArray.push(
      this.formBuilder.control({})
    );
    this.cd.detectChanges();
    this.sessionsFormArray.updateValueAndValidity();
  }

  remove(index: number) {
    this.sessionsFormArray.removeAt(index);
  }
}
