import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormArray, FormBuilder, NG_VALUE_ACCESSOR } from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { IPortDisplay } from "../../interfaces/course-display";
import { IContainerPort } from "../../../shell/interfaces/container-port";

@Component({
  selector: 'app-course-display-custom-url',
  templateUrl: './course-display-custom-url.component.html',
  styleUrls: ['./course-display-custom-url.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CourseDisplayCustomUrlComponent),
      multi: true
    }
  ]
})
export class CourseDisplayCustomUrlComponent implements OnInit, ControlValueAccessor, OnDestroy {
  @Input() ports: IContainerPort[] = [];

  readonly formArray: FormArray;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly formBuilder: FormBuilder,
  ) {
    this.formArray = formBuilder.array([]);
  }

  ngOnInit(): void {
    this.formArray.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
        v => this.propagateChange(v)
      );
  }

  remove(index: number) {
    this.formArray.removeAt(index);
  }

  addCustomDisplay() {
    this.formArray.push(
      this.formBuilder.control({})
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

  writeValue(obj: IPortDisplay[]): void {
    // remove controls
    for (let i = this.formArray.length - 1; i >= 0; i--) {
      this.formArray.removeAt(i);
    }
    // set new value
    for (let i = 0; i < obj.length; i++) {
      this.formArray.setControl(
        i,
        this.formBuilder.control(obj[i]),
      );
    }
  }

  private propagateChange(_: any) {}

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formArray.disable();
    } else {
      this.formArray.enable();
    }
  }
}
