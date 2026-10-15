import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from "@angular/platform-browser";
import { MatSelect } from "@angular/material/select";

import { CoursesListComponent } from './courses-list.component';
import { AdminCourseModule } from "../../admin-course.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { NgxPermissionsModule, NgxPermissionsService } from "ngx-permissions";
import { RouterTestingModule } from "@angular/router/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

import { MatNativeDateModule } from '@angular/material/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import localEn from '@angular/common/locales/en';
import { LOCALE_ID } from '@angular/core';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

registerLocaleData(localeFr);
registerLocaleData(localEn);

describe('CoursesListComponent', () => {
  let component: CoursesListComponent;
  let fixture: ComponentFixture<CoursesListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CoursesListComponent],
      imports: [
        TranslateTestingModule,
        AdminCourseModule,
        HttpClientTestingModule,
        NgxPermissionsModule.forRoot(),
        RouterTestingModule,
        NoopAnimationsModule,
        MatNativeDateModule
      ],
      providers: [
        { provide: LOCALE_ID, useValue: 'fr' }
      ]
    })
    .compileComponents();
  });

  async function render(permissions: string[]) {
    TestBed.inject(NgxPermissionsService).loadPermissions(permissions);
    fixture = TestBed.createComponent(CoursesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function statusFilterValues(): string[] {
    const select = fixture.debugElement
      .queryAll(By.directive(MatSelect))
      .map(de => de.injector.get(MatSelect))
      .find(s => s.ngControl?.name === 'status');
    expect(select).toBeTruthy();
    select!.open();
    fixture.detectChanges();
    return select!.options.map(o => o.value);
  }

  it('should create', async () => {
    await render([]);
    expect(component).toBeTruthy();
  });

  it('does not offer DELETED in a teacher\'s status filter', async () => {
    await render(['ROLE_TEACHER']);
    expect(statusFilterValues()).toEqual(['DRAFT', 'TEST', 'READY', 'ARCHIVED']);
  });

  it('offers DELETED in an admin\'s status filter', async () => {
    await render(['ROLE_ADMIN', 'ROLE_TEACHER']);
    expect(statusFilterValues()).toEqual(['DRAFT', 'TEST', 'READY', 'ARCHIVED', 'DELETED']);
  });
});
