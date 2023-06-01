import { ChangeDetectorRef, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
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
import { forkJoin, Subject } from "rxjs";
import { IRegionWorker } from "../../interfaces/region-worker";
import { IOvhResource } from "../../../sessions-resources/interfaces/ovh-resource";
import { SessionWithResourcesApiService } from "../../../sessions-resources/services/session-with-resources-api.service";
import { ISessionWithResources } from "../../../sessions-resources/interfaces/session-with-resources";
import { takeUntil } from "rxjs/operators";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';

@Component({
  selector: 'app-launch-workers-form',
  templateUrl: './launch-workers-form.component.html',
  styleUrls: ['./launch-workers-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LaunchWorkersFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => LaunchWorkersFormComponent),
      multi: true,
    },
  ],
})
export class LaunchWorkersFormComponent implements OnInit, ControlValueAccessor, OnDestroy, Validator {
  @Input() set selectedSessions(sessionsId: number[]) {
    forkJoin(
      sessionsId.map(id => this.sessionWithResourcesApiService.getSession(id))
    ).subscribe(
      s => {
        this.sessionsWithResources = s;
        this.preComputeResource();
      }
    )
  }
  @Input() resources: IOvhResource[] = [];
  @Input() regions: string[] = [];
  @Input() computeTypes: IComputeType[] = [];

  sessionsWithResources: ISessionWithResources[] = [];
  readonly regionWorkersForm: FormArray;

  private readonly destroy$ = new Subject<void>();
  private humanInteract = false;

  constructor(
    private readonly sessionWithResourcesApiService: SessionWithResourcesApiService,
    private readonly formBuilder: FormBuilder,
    private readonly cd: ChangeDetectorRef,
  ) {
    this.regionWorkersForm = formBuilder.array([], Validators.required)
  }

  ngOnInit(): void {
    this.regionWorkersForm.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
      v => this.propagateChanges(v)
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  registerOnChange(fn: any): void {
    this.propagateChanges = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.regionWorkersForm.invalid) {
      return this.regionWorkersForm.errors;
    } else {
      return null;
    }
  }

  writeValue(obj: IRegionWorker[]): void {
    // remove controls
    for (let i = this.regionWorkersForm.length - 1; i >= 0; i--) {
      this.regionWorkersForm.removeAt(i);
    }
    // set new value
    for (let i = 0; i < obj.length; i++) {
      this.regionWorkersForm.setControl(
        i,
        this.formBuilder.control(obj[i]),
      );
    }
  }

  addRegionWorker() {
    this.humanInteract = true;
    this.regionWorkersForm.push(
      this.formBuilder.control({})
    );
    this.cd.detectChanges();
    this.regionWorkersForm.updateValueAndValidity();
  }

  remove(index: number) {
    this.regionWorkersForm.removeAt(index);
    this.humanInteract = true;
  }

  private propagateChanges(_: IRegionWorker[]) {}

  findResource(resourceId: number): IOvhResource | undefined {
    return this.resources.find(r => r.id === resourceId);
  }

  private preComputeResource() {
    if (this.regionWorkersForm.value.find((w: IRegionWorker) => w.id !== null) || this.humanInteract) {
      return;
    } else {
      this.regionWorkersForm.clear();
    }

    const requiredResources: {[id: number]: number} = {};
    for (const session of this.sessionsWithResources) {
      for (const resource of session.resources) {
        if (!(resource.ovhResourceId in requiredResources)) {
          requiredResources[resource.ovhResourceId] = resource.count;
        } else {
          requiredResources[resource.ovhResourceId] += resource.count;
        }
      }
    }
    const regionCount = this.regions.length;
    for (const [resourceId, count] of Object.entries(requiredResources)) {
      const countByRegion = Math.floor(count / regionCount);
      for (const region of this.regions) {
        this.regionWorkersForm.push(
          this.formBuilder.control({
            id: null,
            region: region,
            resource: resourceId,
            count: countByRegion
          })
        )
      }
    }
    this.cd.detectChanges();
    this.regionWorkersForm.updateValueAndValidity();
  }
}
