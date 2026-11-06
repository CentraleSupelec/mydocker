import {
  Component,
  forwardRef,
  Inject,
  Input,
  OnDestroy,
  OnInit,
} from '@angular/core';
import { APP_CONFIG, IAppConfig } from '../../../../app-config';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators,
} from '@angular/forms';
import { ISlurmCluster } from '../../interfaces/slurm-cluster';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'app-slurm-cluster-form',
  templateUrl: './slurm-cluster-form.component.html',
  styleUrls: ['./slurm-cluster-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SlurmClusterFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SlurmClusterFormComponent),
      multi: true,
    },
  ],
})
export class SlurmClusterFormComponent
  implements OnInit, ControlValueAccessor, OnDestroy, Validator
{
  slurmClusterForm: FormGroup;
  // On edit the secret is optional: leaving it blank keeps the stored secret,
  // which is never sent back to the client. On create it is
  // required.
  @Input() isEdit = false;
  private propagateChange = (_: ISlurmCluster) => {};
  private destroy$: Subject<void> = new Subject<void>();

  constructor(
    private readonly fb: FormBuilder,
    @Inject(APP_CONFIG) readonly config: IAppConfig
  ) {
    this.slurmClusterForm = fb.group({
      displayName: ['', Validators.required],
      // technicalName is required: a blank value is rejected by the
      // unique @NotBlank column server-side and would make a second cluster
      // impossible to create.
      technicalName: ['', Validators.required],
      apiEndpoint: ['', Validators.required],
      secretKey: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    if (this.isEdit) {
      const secretControl = this.slurmClusterForm.get('secretKey');
      secretControl?.clearValidators();
      secretControl?.updateValueAndValidity({ emitEvent: false });
    }
    this.slurmClusterForm.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.propagateChange(this.slurmClusterForm.getRawValue());
      });
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.slurmClusterForm.invalid) {
      return {
        slurmClusterErrors: this.slurmClusterForm.errors,
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
      this.slurmClusterForm.disable({ emitEvent: false });
    } else {
      this.slurmClusterForm.enable();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
  }

  writeValue(obj?: ISlurmCluster): void {
    this.slurmClusterForm.setValue({
      displayName: obj?.displayName || '',
      technicalName: obj?.technicalName || '',
      apiEndpoint: obj?.apiEndpoint || '',
      secretKey: obj?.secretKey || '',
    });
  }
}
