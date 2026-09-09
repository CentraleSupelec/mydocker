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
import { IResourceRegions } from "../../../sessions-resources/interfaces/session-with-resources";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";

@Component({
  selector: 'app-resource-regions-row',
  templateUrl: './resource-regions-row.component.html',
  styleUrls: ['./resource-regions-row.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ResourceRegionsRowComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ResourceRegionsRowComponent),
      multi: true,
    },
  ]
})
export class ResourceRegionsRowComponent implements OnInit, ControlValueAccessor, Validator {
  readonly resourceRegionsFormGroup: FormGroup;
  @Input() resources: IOvhResource[] = [];
  @Input() regions: string[] = [];

  private propagateChange = (_: IResourceRegions) => {};
  constructor(
    formBuilder: FormBuilder,
  ) {
    this.resourceRegionsFormGroup = formBuilder.group({
      ovhResourceId: [null, Validators.required],
      regions: [null, Validators.required],
    })
  }

  ngOnInit(): void {
    this.resourceRegionsFormGroup.valueChanges
      .subscribe(
        v => this.propagateChange({
          regions: v.regions,
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
    if (this.resourceRegionsFormGroup.invalid) {
      return {resourceRegionsErrors: this.resourceRegionsFormGroup.errors};
    }
    return null;
  }

  writeValue(obj: IResourceRegions): void {
    this.resourceRegionsFormGroup.setValue({
      ovhResourceId: obj?.ovhResourceId !== undefined? String(obj?.ovhResourceId): null,
      regions: obj?.regions || [],
    });
  }
}
