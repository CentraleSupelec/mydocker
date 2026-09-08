import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CoursesAdminComponent } from './courses-admin.component';
import { AdminCourseModule } from '../../admin-course.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NgxPermissionsModule } from 'ngx-permissions';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('CoursesAdminComponent', () => {
  let component: CoursesAdminComponent;
  let fixture: ComponentFixture<CoursesAdminComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CoursesAdminComponent ],
      imports: [
        TranslateTestingModule,
        AdminCourseModule,
        HttpClientTestingModule,
        NgxPermissionsModule.forRoot(),
        RouterTestingModule,
        NoopAnimationsModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CoursesAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
