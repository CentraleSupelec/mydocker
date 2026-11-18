import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';

import { UserEditComponent } from './user-edit.component';
import { UserFormComponent } from '../user-form/user-form.component';
import { AdminUsersModule } from '../../admin-users.module';
import { UserApiService } from '../../service/user-api.service';
import { ObservableSnackNotificationService } from '../../../utils/snack-notification/observable-snack-notification.service';
import { APP_CONFIG } from '../../../../app-config';
import { IUser } from '../../../permissions/interfaces/user';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

/**
 * CAS login stores the bare principal (for example "casstudent") in both username and email of the
 * user row. The admin form must accept it, so that an administrator can change the role of a
 * CAS-provisioned user: a form that finds the stored email invalid keeps Save disabled.
 */
describe('UserEditComponent with a CAS-provisioned user', () => {
  const casUser = {
    id: 11,
    username: 'casstudent',
    email: 'casstudent',
    name: 'Cas',
    lastname: 'Student',
    enabled: true,
    roles: ['ROLE_USER'],
  } as IUser;

  let fixture: ComponentFixture<UserEditComponent>;
  let editUser: jasmine.Spy;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UserEditComponent ],
      providers: [
        { provide: APP_CONFIG, useValue: {} },
        { provide: ActivatedRoute, useValue: { data: of({ user: casUser }) } },
      ],
      imports: [
        TranslateTestingModule,
        HttpClientTestingModule,
        AdminUsersModule,
        RouterTestingModule,
        NoopAnimationsModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    editUser = spyOn(TestBed.inject(UserApiService), 'editUser').and.returnValue(of(undefined));
    spyOn(TestBed.inject(ObservableSnackNotificationService), 'toast');
    fixture = TestBed.createComponent(UserEditComponent);
    fixture.detectChanges();
  });

  // The admin picks another role in the form, which is what the select does underneath.
  function change(values: Partial<IUser>): void {
    const form: UserFormComponent = fixture.debugElement.query(By.directive(UserFormComponent)).componentInstance;
    form.userForm.patchValue(values);
    fixture.detectChanges();
  }

  function saveButton(): HTMLButtonElement {
    return fixture.debugElement.query(By.css('button[mat-raised-button]')).nativeElement;
  }

  it('enables Save after a role change when the stored email is a bare principal', () => {
    change({ roles: ['ROLE_TEACHER'] });

    expect(saveButton().disabled).toBeFalse();
  });

  it('sends the stored username and email unchanged with the new role', () => {
    change({ roles: ['ROLE_TEACHER'] });

    saveButton().click();

    expect(editUser).toHaveBeenCalledWith(11, jasmine.objectContaining({
      username: 'casstudent',
      email: 'casstudent',
      roles: ['ROLE_TEACHER'],
    }));
  });

  it('keeps Save disabled when the email is emptied', () => {
    change({ email: '' });

    expect(saveButton().disabled).toBeTrue();
  });

  it('sends an email the admin typed', () => {
    change({ email: 'cas.student@example.org' });

    saveButton().click();

    expect(editUser).toHaveBeenCalledWith(11, jasmine.objectContaining({
      username: 'casstudent',
      email: 'cas.student@example.org',
    }));
  });
});
