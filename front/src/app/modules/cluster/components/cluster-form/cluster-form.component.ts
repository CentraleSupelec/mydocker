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
import { ICluster } from '../../interfaces/cluster';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-cluster-form',
  templateUrl: './cluster-form.component.html',
  styleUrls: ['./cluster-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ClusterFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => ClusterFormComponent),
      multi: true,
    },
  ]
})
export class ClusterFormComponent implements OnInit, ControlValueAccessor, OnDestroy, Validator {
  clusterForm: FormGroup;
  private propagateChange = (_: ICluster) => {};
  private destroy$: Subject<void> = new Subject<void>();


  constructor(
    private readonly fb: FormBuilder,
    @Inject(APP_CONFIG) readonly config: IAppConfig
  ) {

    this.clusterForm = fb.group({
      name: ['', Validators.required],
      goAddress: [null],
      goPort: [null],
      terraformStateUrl: [null],
      deploymentEnabled: [false, Validators.required],
    });
  }

  ngOnInit(): void {
    this.clusterForm.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.propagateChange(this.clusterForm.getRawValue())
      })
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.clusterForm.invalid) {
      return {
        clusterErrors: this.clusterForm.errors
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
      this.clusterForm.disable({ emitEvent: false });
    } else {
      this.clusterForm.enable();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
  }

  writeValue(obj?: ICluster): void {
    this.clusterForm.setValue({
      name: obj?.name || '',
      goAddress: obj?.goAddress || '',
      goPort: obj?.goPort == null ? null : obj?.goPort,
      terraformStateUrl: obj?.terraformStateUrl || '',
      deploymentEnabled: obj?.deploymentEnabled !== undefined ? obj.deploymentEnabled : false,
    })
  }
}
