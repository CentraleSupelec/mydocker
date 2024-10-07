import { Component, forwardRef, OnDestroy, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup, NG_VALIDATORS, NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator, Validators
} from "@angular/forms";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { Roles } from "../../interfaces/roles";

@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UserFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => UserFormComponent),
      multi: true,
    },
  ]
})
export class UserFormComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {
  readonly userForm: FormGroup;
  private readonly destroy$ = new Subject<void>();
  readonly roles = Object.entries(Roles)

  constructor(
    formBuilder: FormBuilder,
  ) {
    this.userForm = formBuilder.group({
      username: ['', Validators.required],
      name: ['', Validators.required],
      lastname: ['', Validators.required],
      email: ['', [Validators.email, Validators.required]],
      enabled: [true],
      role: 'ROLE_TEACHER'
    })
  }

  ngOnInit(): void {
    this.userForm.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(v => this.propagateChange(v))
  }

  ngOnDestroy(): void {
    this.destroy$.next()
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.userForm.invalid) {
      return {'user': this.userForm.errors}
    }
    return null;
  }

  writeValue(obj: any): void {
    this.userForm.setValue({
      username: obj?.username || '',
      name: obj?.name || '',
      lastname: obj?.lastname || '',
      email: obj?.email || '',
      enabled: obj?.enabled ?? true,
      role: obj?.role || 'ROLE_TEACHER'
    })
  }

  private propagateChange(_: any) {}
}
