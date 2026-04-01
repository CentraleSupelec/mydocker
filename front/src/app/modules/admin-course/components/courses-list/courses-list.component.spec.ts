import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CoursesListComponent } from './courses-list.component';
import { AdminCourseModule } from "../../admin-course.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { NgxPermissionsModule } from "ngx-permissions";
import { RouterTestingModule } from "@angular/router/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

import { MatNativeDateModule } from '@angular/material/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';

registerLocaleData(localeFr);

describe('CoursesListComponent', () => {
  let component: CoursesListComponent;
  let fixture: ComponentFixture<CoursesListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CoursesListComponent],
      imports: [
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

  beforeEach(() => {
    fixture = TestBed.createComponent(CoursesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
