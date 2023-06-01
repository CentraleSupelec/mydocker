import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor, FormBuilder, FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator, Validators
} from "@angular/forms";
import { ICourseDisplay } from "../../interfaces/course-display";
import { IPort } from "../../../ports-form/interfaces/port";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { IContainer } from "../../../shell/interfaces/container";
import { tap } from "rxjs/operators";

@Component({
  selector: 'app-course-display-form',
  templateUrl: './course-display-form.component.html',
  styleUrls: ['./course-display-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CourseDisplayFormComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CourseDisplayFormComponent),
      multi: true
    }
  ]
})
export class CourseDisplayFormComponent implements OnInit, ControlValueAccessor, OnDestroy, Validator {
  @Input() set ports(ports :IPort[]) {
    this.container.ports = ports.map(v => ({
      ...v,
      portMapTo: v.mapPort
    }));
  };

  readonly formGroup: FormGroup;
  container: IContainer = {
    username: 'USERNAME',
    password: 'PASSWORD',
    ip: 'IP_ADDRESS',
    ports: [],
    status: 'OK'
  };

  private readonly destroy$ = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
  ) {
    this.formGroup = formBuilder.group({
      displayUsername: [true, Validators.required],
      displayPassword: [true, Validators.required],
      displayPorts: formBuilder.control({}),
      customPortsDisplay: [[]],
    })
  }

  ngOnInit(): void {
    this.formGroup.valueChanges
      .pipe(
        takeUntil(this.destroy$),
      )
      .subscribe(
        v => this.propagateChange(v)
      );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.formGroup.invalid) {
      return {courseDisplay: this.formGroup.errors};
    }
    return null;
  }

  writeValue(obj: ICourseDisplay): void {
    this.formGroup.setValue({
      displayUsername: obj?.displayUsername !== undefined ? obj.displayUsername : true,
      displayPassword: obj?.displayPassword !== undefined ? obj.displayPassword : true,
      displayPorts: obj?.displayPorts || {},
      customPortsDisplay: obj?.customPortsDisplay || [],
    })
  }

  private propagateChange(_: ICourseDisplay) {}

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formGroup.disable();
    } else {
      this.formGroup.enable();
    }
  }
}
