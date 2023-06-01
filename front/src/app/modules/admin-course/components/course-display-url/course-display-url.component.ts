import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from "@angular/forms";
import { RenderStringService } from "../../../display-container/render-string.service";
import { IContainerPort } from "../../../shell/interfaces/container-port";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { IHttpPortDisplay } from "../../interfaces/course-display";

@Component({
  selector: 'app-course-display-url',
  templateUrl: './course-display-url.component.html',
  styleUrls: ['./course-display-url.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CourseDisplayUrlComponent),
      multi: true
    }
  ]
})
export class CourseDisplayUrlComponent implements OnInit, ControlValueAccessor, OnDestroy {
  @Input() ports: IContainerPort[] = [];

  readonly formGroup: FormGroup;
  private readonly destroy$ = new Subject<void>();

  constructor(
    formBuilder: FormBuilder,
    private readonly renderStringService: RenderStringService,
  ) {
    this.formGroup = formBuilder.group({
      title: '',
      url: ''
    });
  }

  ngOnInit(): void {
    this.formGroup.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
        v => this.propagateChange({
          ...v,
          type: 'HTTP'
        })
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

  writeValue(obj: IHttpPortDisplay): void {
    this.formGroup.setValue({
      title: obj?.title || '',
      url: obj?.url || ''
    })
  }

  renderString(url: string): string {
    return this.renderStringService.renderString(
      url, this.ports, 'USERNAME', 'PASSWORD', 'IP_ADDRESS'
    )
  }

  private propagateChange(_ : any) {}

}
