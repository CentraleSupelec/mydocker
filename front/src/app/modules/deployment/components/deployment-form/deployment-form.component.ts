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
import { IDeploymentSession } from "../../interfaces/deployment-session";
import { Subject } from "rxjs";
import { mergeMap, takeUntil } from "rxjs/operators";
import { DeploymentApiService } from "../../services/deployment-api.service";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";
import { tap } from "rxjs/operators";
import { IDeployment } from "../../interfaces/deployment";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';
import { ICluster } from 'src/app/modules/cluster/interfaces/cluster';

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
  @Input() clusters: ICluster[] = [];

  readonly deploymentForm: FormGroup;
  sessions: IDeploymentSession[] = [];
  selectedSessions: IDeploymentSession[] = [];
  filteredComputeTypes: IComputeType[] = [];

  private readonly destroy$ = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
    private readonly deploymentApiService: DeploymentApiService,
  ) {
    this.deploymentForm = formBuilder.group({
      type: ['', Validators.required],
      description: '',
      sessions: [[]],
      cluster: [null, Validators.required],
      startDateTime: [null, Validators.required],
      workers: [[], Validators.required],
    });
  }

  ngOnInit(): void {
    this.deploymentForm.get('cluster')?.valueChanges.subscribe(
      (cluster: number) => {
        if (cluster !== null) {
          this.filteredComputeTypes = this.computeTypes
            .filter(computeType => computeType.cluster?.id == cluster);
        }
        this.reconcileSessions();
      }
    )
    this.deploymentForm.get('type')?.valueChanges
      .pipe(
        tap(() => {
          this.deploymentForm.get('sessions')?.setValue([]);
          this.deploymentForm.get('cluster')?.setValue(null);
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
      .subscribe(s => {
        this.sessions = s;
        this.reconcileSessions();
      });

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

  // Sessions of the chosen cluster, plus those whose course has no cluster yet; all before a cluster is chosen.
  get sessionsForCluster(): IDeploymentSession[] {
    const cluster = this.deploymentForm.get('cluster')?.value;
    return cluster == null
      ? this.sessions
      : this.sessions.filter(session => this.onCluster(session, cluster));
  }

  // Run when the cluster or the session list changes. The back rejects a session of another
  // cluster, so a selected one known to be elsewhere is dropped. An id not in the list yet is kept:
  // on edit the cluster is set before the list arrives, and the arrival runs this again.
  private reconcileSessions() {
    const cluster = this.deploymentForm.get('cluster')?.value;
    const sessionsControl = this.deploymentForm.get('sessions');
    const selected: number[] = sessionsControl?.value ?? [];
    const kept = cluster == null ? selected : selected.filter(id => {
      const session = this.sessions.find(s => s.id == id);
      return !session || this.onCluster(session, cluster);
    });
    if (kept.length !== selected.length) {
      sessionsControl?.setValue(kept);
    } else {
      // Refresh the summary only: recomputing the start date here would overwrite a saved one on edit.
      this.selectedSessions = this.sessions.filter(session => selected.includes(session.id));
    }
  }

  private onCluster(session: IDeploymentSession, cluster: number): boolean {
    return session.cluster == null || session.cluster == cluster;
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
      description: obj?.description || '',
      cluster: obj?.cluster || null,
    })
  }

  private propagateChanges(_: IDeployment) {}
}
