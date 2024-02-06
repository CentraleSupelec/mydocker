import { ChangeDetectorRef, Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { IPort } from "../../../ports-form/interfaces/port";
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";

@Component({
  selector: 'app-course-display-ports-form',
  templateUrl: './course-display-ports-form.component.html',
  styleUrls: ['./course-display-ports-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CourseDisplayPortsFormComponent),
      multi: true
    }
  ]
})
export class CourseDisplayPortsFormComponent implements OnInit, ControlValueAccessor, OnDestroy {
  private valueHistory: {[id: string]: boolean} = {};

  @Input() set ports(ports: IPort[]) {
    this.valueHistory = {
      ...this.valueHistory,
      ...this.formGroup.value,
    };
    for (let controlsKey in this.formGroup.controls) {
      this.formGroup.removeControl(controlsKey);
    }
    ports.forEach(
      p => {
        if (!p.mapPort) {
          return;
        }
        const key = String(p.mapPort);
        if (key in this.valueHistory) {
          this.formGroup.setControl(
            key,
            this.formBuilder.control({value: this.valueHistory[key], disabled: this.disabled})
          );
        } else {
          this.formGroup.setControl(
            key,
            this.formBuilder.control({value: true, disabled: this.disabled})
          );
        }
      }
    )
    this.cd.detectChanges();
  }


  readonly formGroup: FormGroup;

  private readonly destroy$ = new Subject<void>();
  private disabled = false;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly cd: ChangeDetectorRef,
  ) {
    this.formGroup = formBuilder.group({});
  }

  ngOnInit(): void {
    this.formGroup.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
        v => this.propagateChange(v)
      )
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(obj: {[id: string]: boolean}): void {
    for (let objKey in obj) {
      this.formGroup.setControl(
        objKey,
        this.formBuilder.control({value: obj[objKey], disabled: this.disabled})
      );
    }
  }

  private propagateChange(_: any) {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.formGroup.disable();
    } else {
      this.formGroup.enable();
    }
  }
}
