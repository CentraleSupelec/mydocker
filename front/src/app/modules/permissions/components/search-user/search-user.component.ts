import { Component, forwardRef, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormControl, NG_VALUE_ACCESSOR, Validators } from "@angular/forms";
import { Subject } from "rxjs";
import { IUser } from "../../interfaces/user";
import { UserApiServiceService } from "../../services/user-api-service.service";
import { debounceTime, filter, switchMap } from "rxjs/operators";
import { takeUntil } from "rxjs/operators";

@Component({
  selector: 'app-search-user',
  templateUrl: './search-user.component.html',
  styleUrls: ['./search-user.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SearchUserComponent),
      multi: true
    }
  ]
})
export class SearchUserComponent implements OnInit, OnDestroy, ControlValueAccessor {
  readonly inputControl: FormControl;
  readonly filterUser$ = new Subject<IUser[]>();
  readonly destroy$ = new Subject<void>();

  constructor(
    fb: FormBuilder,
    private readonly userApiService: UserApiServiceService,
  ) {
    this.inputControl = fb.control('');
  }

  ngOnInit(): void {
    this.inputControl.valueChanges
      .pipe(
        debounceTime(300),
        filter(v => v !== null && typeof v === 'string' && v.length > 0),
        switchMap(v => this.userApiService.searchUser(v)),
        takeUntil(this.destroy$),
      ).subscribe(
        users => this.filterUser$.next(users)
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

  displayFn(user: IUser): string {
    return user && user.email ? user.email : '';
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(obj: IUser): void {
    if (obj !== null) {
      this.filterUser$.next([obj]);
      this.inputControl.setValue(obj);
    } else {
      this.inputControl.setValue('');
    }
  }

  propagateChange(_: number) {}
}
