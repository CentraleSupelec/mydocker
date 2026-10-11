import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR, ValidationErrors,
  Validator,
  Validators
} from "@angular/forms";
import { ISession } from "../../../shell/interfaces/session";
import { Subject } from "rxjs";
import { mergeMap, takeUntil } from "rxjs/operators";
import { DeploymentApiService } from "../../services/deployment-api.service";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";
import { tap } from "rxjs/operators";
import { IDeployment } from "../../interfaces/deployment";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';

@Component({
  selector: 'app-deployment-form',
  templateUrl: './deployment-form.component.html',
  styleUrls: ['./deployment-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DeploymentFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => DeploymentFormComponent),
      multi: true
    }
  ]
})
export class DeploymentFormComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {
  @Input() resources: IOvhResource[] = [];
  @Input() regions: string[] = [];
  @Input() computeTypes: IComputeType[] = [];

  readonly deploymentForm: FormGroup;
  sessions: ISession[] = [];
  selectedSessions: ISession[] = [];

  private readonly destroy$ = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
    private readonly deploymentApiService: DeploymentApiService,
  ) {
    this.deploymentForm = formBuilder.group({
      type: ['', Validators.required],
      description: '',
      sessions: [[]],
      startDateTime: [null, Validators.required],
      workers: [[], Validators.required],
    });
  }

  ngOnInit(): void {
    this.deploymentForm.get('type')?.valueChanges
      .pipe(
        tap(() => {
          this.deploymentForm.get('sessions')?.setValue([]);
          this.deploymentForm.get('startDateTime')?.setValue(null);
          this.deploymentForm.get('workers')?.setValue([]);
        }),
        mergeMap(type => {
          if (type === 'launch') {
            return this.deploymentApiService.getSessionToLaunch();
          } else {
            return this.deploymentApiService.getSessionToClean();
          }
        }),
        takeUntil(this.destroy$)
      )
      .subscribe(s => this.sessions = s);

    this.deploymentForm.get('sessions')?.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      ).subscribe(
      sessionsId => {
        this.selectedSessions = this.sessions.filter(
          session => sessionsId.includes(session.id)
        )
        this.computeStartDateTime();
      }
    );

    this.deploymentForm.valueChanges
      .pipe(
        takeUntil(this.destroy$),
      )
      .subscribe(
        v => this.propagateChanges(v)
      );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  private computeStartDateTime() {
    if (this.selectedSessions.length === 0) {
      return
    }

    if (this.deploymentForm.get('type')?.value === 'launch') {
      this.deploymentForm.get('startDateTime')?.setValue(
        Math.min(...this.selectedSessions.map(s => s.startDateTime)) - 3_600_000
      );
    }
    if (this.deploymentForm.get('type')?.value === 'clean') {
      this.deploymentForm.get('startDateTime')?.setValue(
        Math.max(...this.selectedSessions.map(s => s.startDateTime)) + 3_600_000
      );
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChanges = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.deploymentForm.invalid) {
      return this.deploymentForm.errors;
    }
    return null;
  }

  writeValue(obj: IDeployment): void {
    this.deploymentForm.setValue({
      type: obj?.type || '',
      sessions: obj?.sessions || [],
      startDateTime: obj?.startDateTime || null,
      workers: obj?.workers || [],
      description: obj?.description || ''
    })
  }

  private propagateChanges(_: IDeployment) {}
}
