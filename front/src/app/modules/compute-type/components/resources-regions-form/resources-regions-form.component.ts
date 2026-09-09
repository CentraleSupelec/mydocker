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
import { IResourceRegions } from "../../../sessions-resources/interfaces/session-with-resources";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";

@Component({
  selector: 'app-resources-regions-form',
  templateUrl: './resources-regions-form.component.html',
  styleUrls: ['./resources-regions-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ResourcesRegionsFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ResourcesRegionsFormComponent),
      multi: true,
    },
  ]
})
export class ResourcesRegionsFormComponent implements OnInit, ControlValueAccessor, Validator {
  readonly computeTypeFormArray: FormArray;
  @Input() resources: IOvhResource[] = [];
  @Input() regions: string[] = [];

  private propagateChange = (_: IResourceRegions[]) => {};

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly cd: ChangeDetectorRef,
  ) {
    this.computeTypeFormArray = formBuilder.array([]);
  }

  ngOnInit(): void {
    this.computeTypeFormArray.valueChanges
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
    if (this.computeTypeFormArray.invalid) {
      return { resourcesRegions: this.computeTypeFormArray.errors }
    }
    return null;
  }

  writeValue(obj: IResourceRegions[]): void {
    // remove controls
    for (let i = this.computeTypeFormArray.length - 1; i >= 0; i--) {
      this.computeTypeFormArray.removeAt(i);
    }
    // set new value
    for (let i = 0; i < obj?.length; i++) {
      this.computeTypeFormArray.setControl(
        i,
        this.formBuilder.control(obj[i]),
      );
    }
  }

  addResource() {
    this.computeTypeFormArray.push(
      this.formBuilder.control({})
    );
    this.cd.detectChanges();
    this.computeTypeFormArray.updateValueAndValidity();
  }

  remove(index: number) {
    this.computeTypeFormArray.removeAt(index);
  }
}
