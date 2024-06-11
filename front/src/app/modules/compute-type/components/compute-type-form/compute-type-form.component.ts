import { Component, forwardRef, Inject, Input, OnDestroy, OnInit } from '@angular/core';
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR, ValidationErrors,
  Validator, Validators
} from '@angular/forms';
import { IComputeType } from '../../interfaces/compute-type';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { IOvhResource } from '../../../sessions-resources/interfaces/ovh-resource';

@Component({
  selector: 'app-compute-type-form',
  templateUrl: './compute-type-form.component.html',
  styleUrls: ['./compute-type-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ComputeTypeFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ComputeTypeFormComponent),
      multi: true,
    },
  ]
})
export class ComputeTypeFormComponent implements OnInit, ControlValueAccessor, OnDestroy, Validator {
  @Input() resources: IOvhResource[] = [];
  @Input() regions: string[] = [];
  computeTypeForm: FormGroup;
  private propagateChange = (_: IComputeType) => {};
  private destroy$: Subject<void> = new Subject<void>();


  constructor(
    private readonly fb: FormBuilder,
    @Inject(APP_CONFIG) readonly config: IAppConfig
  ) {

    this.computeTypeForm = fb.group({
      displayName: ['', Validators.required],
      technicalName: [''],
      gpu: [false, Validators.required],
      autoscalingRegions: [null],
      autoscalingResource: [null],
      minIdleNodesCount: [null],
      maxNodesCount: [null],
      manualNodesCount: [null],
    });
  }

  ngOnInit(): void {
    this.computeTypeForm.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.propagateChange(this.computeTypeForm.getRawValue())
      })
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.computeTypeForm.invalid) {
      return {
        computeTypeErrors: this.computeTypeForm.errors
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
      this.computeTypeForm.disable({ emitEvent: false });
    } else {
      this.computeTypeForm.enable();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
  }

  writeValue(obj?: IComputeType): void {
    this.computeTypeForm.setValue({
      displayName: obj?.displayName || '',
      technicalName: obj?.technicalName || '',
      gpu: obj?.gpu || false,
      autoscalingRegions: obj?.autoscalingRegions?.map(ovhRegion => ovhRegion.region) || [],
      autoscalingResource: obj?.autoscalingResource || null,
      minIdleNodesCount: obj?.minIdleNodesCount == null ? null : obj?.minIdleNodesCount,
      maxNodesCount: obj?.maxNodesCount == null ? null : obj?.maxNodesCount,
      manualNodesCount: obj?.manualNodesCount == null ? null : obj?.manualNodesCount,
    })
  }

}
