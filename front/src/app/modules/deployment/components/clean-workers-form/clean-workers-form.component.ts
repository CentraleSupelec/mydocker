import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { forkJoin, Subject } from "rxjs";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";
import { DeploymentApiService } from "../../services/deployment-api.service";
import { IRegionWorker, IRegionWorkerWithSession } from "../../interfaces/region-worker";
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from "@angular/forms";
import { filter, switchMap } from "rxjs/operators";
import { takeUntil } from "rxjs/operators";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';

@Component({
  selector: 'app-clean-workers-form',
  templateUrl: './clean-workers-form.component.html',
  styleUrls: ['./clean-workers-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CleanWorkersFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CleanWorkersFormComponent),
      multi: true
    }
  ]
})
export class CleanWorkersFormComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {
  @Input() set selectedSessions(sessions: number[]) {
    this.sessions = sessions;
    this.fetchRegionWorkerToClean$.next();
  }
  @Input() resources: IOvhResource[] = [];
  @Input() set startDate(date: number) {
    this.date = date;
    this.fetchRegionWorkerToClean$.next();
  }
  @Input() computeTypes: IComputeType[] = [];

  sessions: number[] = [];
  date: number | null = null;
  private readonly fetchRegionWorkerToClean$ = new Subject<void>();
  private readonly destroy$ = new Subject<void>();
  private regionWorkersMap: Map<number, IRegionWorkerWithSession> = new Map<number, IRegionWorkerWithSession>();

  readonly workerToClean: FormArray;

  constructor(
    private readonly deploymentApiService: DeploymentApiService,
    private readonly formBuilder: FormBuilder,
  ) {
    this.workerToClean = formBuilder.array([], Validators.required);
  }

  ngOnInit(): void {
    this.fetchRegionWorkerToClean$.pipe(
      filter(() => this.date !== null),
      switchMap(() => forkJoin(
        [
          ...this.sessions.map(id => this.deploymentApiService.getRegionWorkerFromSession(id)),
          this.deploymentApiService.getRegionWorkerToClean(<number>this.date)
          ]
        )
      ),
      takeUntil(this.destroy$)
    ).subscribe(
      workers => {
        this.regionWorkersMap.clear();
        workers.forEach(worker =>
          worker.forEach(w =>
            this.regionWorkersMap.set(w.id, w)
          )
        );
        this.workerToClean.clear();
        for (const w of this.regionWorkersMap.values()) {
          this.workerToClean.push(
            this.formBuilder.group({
              check: false,
              id: w.id,
              resource: w.resource,
              region: w.region,
              count: w.count,
              computeTypeId: w.computeTypeId,
            })
          );
        }
      }
    )

    this.workerToClean.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(
      v => {
        this.propagateChange(v.filter((w: any) => w.check));
      }
    )
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(control: AbstractControl): ValidationErrors | null {
    return null;
  }

  writeValue(obj: IRegionWorker[]): void {
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  displaySession(id: number): string | undefined {
    return this.regionWorkersMap.get(id)?.sessions.reduce((acc, s, index) =>
      acc + (index === 0 ? '' : ', ') + s.course.title, ''
    );
  }

  private propagateChange(_: any) {}
}
