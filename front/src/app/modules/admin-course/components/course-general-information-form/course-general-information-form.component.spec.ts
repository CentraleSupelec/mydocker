import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from "@angular/platform-browser";
import { MatSelect } from "@angular/material/select";

import { CourseGeneralInformationFormComponent } from './course-general-information-form.component';
import { AdminCourseModule } from "../../admin-course.module";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { NgxPermissionsModule } from "ngx-permissions";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { CourseStatus } from "../../interfaces/course";

describe('CourseGeneralInformationFormComponent', () => {
  let component: CourseGeneralInformationFormComponent;
  let fixture: ComponentFixture<CourseGeneralInformationFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseGeneralInformationFormComponent ],
      imports: [
        TranslateTestingModule,
        AdminCourseModule,
        NoopAnimationsModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseGeneralInformationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function statusSelect(): MatSelect | undefined {
    return fixture.debugElement
      .queryAll(By.directive(MatSelect))
      .map(de => de.injector.get(MatSelect))
      .find(s => s.ngControl?.name === 'status');
  }

  function setStatus(status: CourseStatus): void {
    component.formGroup.get('status')!.setValue(status);
    fixture.detectChanges();
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('does not offer DELETED in the status drop-down', () => {
    const select = statusSelect();
    expect(select).toBeTruthy();

    select!.open();
    fixture.detectChanges();

    expect(select!.options.map(o => o.value)).toEqual(['DRAFT', 'TEST', 'READY', 'ARCHIVED']);
  });

  it('shows DELETED read-only when the course is deleted', () => {
    setStatus(CourseStatus.DELETED);

    expect(statusSelect()!.panelOpen).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('DELETED');
  });

  it('shows no read-only status for a course that is not deleted', () => {
    setStatus(CourseStatus.READY);

    expect(fixture.nativeElement.textContent).not.toContain('DELETED');
  });

  it('still does not offer DELETED in the drop-down when the course is deleted', () => {
    setStatus(CourseStatus.DELETED);
    const select = statusSelect();
    expect(select).toBeTruthy();

    select!.open();
    fixture.detectChanges();

    expect(select!.options.map(o => o.value)).toEqual(['DRAFT', 'TEST', 'READY', 'ARCHIVED']);
  });
});
