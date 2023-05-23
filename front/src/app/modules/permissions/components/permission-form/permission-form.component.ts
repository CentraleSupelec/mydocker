import { Component, forwardRef, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from "@angular/forms";
import { Subject } from "rxjs";
import { IPermission } from "../../interfaces/permission";

@Component({
  selector: 'app-permission-form',
  templateUrl: './permission-form.component.html',
  styleUrls: ['./permission-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PermissionFormComponent),
      multi: true
    }
  ]
})
export class PermissionFormComponent implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  readonly formGroup: FormGroup;
  @Output() save$ = new Subject<IPermission>();
  @Output() delete$ = new Subject<IPermission>();
  @Input() set permission(obj: IPermission) {
    this.formGroup.setValue({
      user: obj?.user || null,
      type: obj?.type || 'view',
      id: obj?.id || null,
    });
  }

  constructor(
    fb: FormBuilder
  ) {
    this.formGroup = fb.group({
      user: [null, Validators.required],
      type: ['view', Validators.required],
      id: null
    })
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }



  save() {
    this.save$.next(
      this.formGroup.value
    );
    this.formGroup.setValue({
      user: null,
      type: 'view',
      id: null
    });
  }

  delete() {
    this.delete$.next(
      this.formGroup.value
    );
  }
}
