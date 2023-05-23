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
import { IRegionWorker } from "../../interfaces/region-worker";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';

@Component({
  selector: 'app-worker-form',
  templateUrl: './worker-form.component.html',
  styleUrls: ['./worker-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => WorkerFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => WorkerFormComponent),
      multi: true,
    },
  ]
})
export class WorkerFormComponent implements OnInit, ControlValueAccessor, Validator {
  readonly workerFormGroup: FormGroup;
  @Input() resources: IOvhResource[] = [];
  @Input() regions: string[] = [];
  @Input() computeTypes: IComputeType[] = [];
  private defaultComputeTypeId: number | null = null;

  private propagateChange = (_: IRegionWorker) => {};
  constructor(
    formBuilder: FormBuilder,
  ) {
    this.workerFormGroup = formBuilder.group({
      id: null,
      region: [null, Validators.required],
      resource: [null, Validators.required],
      count: [null, Validators.required],
      computeTypeId: [null, []],
    })
  }

  ngOnInit(): void {
    this.workerFormGroup.valueChanges
      .subscribe(
        v => this.propagateChange(v)
      );
    this.defaultComputeTypeId = this.computeTypes
      .find((computeType) => computeType.technicalName === '')
      ?.id || null;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.workerFormGroup.invalid) {
      return {session: this.workerFormGroup.errors};
    }
    return null;
  }

  writeValue(obj: IRegionWorker): void {
    this.workerFormGroup.setValue({
      id: obj?.id || null,
      region: obj?.region || null,
      resource: obj?.resource || null,
      count: obj?.count || null,
      computeTypeId: obj?.computeTypeId || this.defaultComputeTypeId,
    });
  }
}
