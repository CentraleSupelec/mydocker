import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseTechnicalInformationFormComponent } from './course-technical-information-form.component';
import { AdminCourseModule } from "../../admin-course.module";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { APP_CONFIG, IAppConfig } from '../../../../app-config';

describe('CourseTechnicalInformationFormComponent', () => {
  let component: CourseTechnicalInformationFormComponent;
  let fixture: ComponentFixture<CourseTechnicalInformationFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseTechnicalInformationFormComponent ],
      imports: [
        TranslateTestingModule,
        AdminCourseModule,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseTechnicalInformationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // Conformance cases for the command port placeholder grammar. The back-end validator and
  // the Go substitution implement the same table; a change here belongs in all three at once.
  describe('command port validation', () => {
    const setCommandAndPorts = (command: string, mapPorts: number[]) => {
      component.courseTechnicalForm.get('command')?.setValue(command);
      component.courseTechnicalForm.get('ports')?.setValue(
        mapPorts.map((mapPort) => ({ mapPort }))
      );
    };

    const accepts = (command: string) => {
      setCommandAndPorts(command, [8080, 22]);
      expect(component.courseTechnicalForm.hasError('unknownCommandPorts')).toBeFalse();
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeFalse();
    };

    it('accepts a single quoted declared port', () => accepts("serve --port {{PORT['8080']}}"));

    it('accepts a double quoted declared port', () => accepts('serve --port {{PORT["8080"]}}'));

    it('accepts several declared ports', () => accepts("{{PORT['8080']}} {{PORT['22']}}"));

    it('accepts a command without a placeholder', () => accepts('sleep infinity'));

    it('accepts other placeholders', () => accepts('login {{USERNAME}} {{PASSWORD}} {{IP}}'));

    it('accepts an empty command', () => accepts(''));

    it('rejects an undeclared port', () => {
      setCommandAndPorts("serve --port {{PORT['9999']}}", [8080]);
      expect(component.courseTechnicalForm.getError('unknownCommandPorts').invalidPorts)
        .toEqual(['9999']);
    });

    it('rejects an unquoted placeholder as malformed', () => {
      setCommandAndPorts('serve --port {{PORT[8080]}}', [8080]);
      expect(component.courseTechnicalForm.getError('malformedCommandPorts').placeholders)
        .toEqual(['{{PORT[8080]}}']);
    });

    it('rejects mismatched quotes as malformed', () => {
      setCommandAndPorts('serve --port {{PORT[\'8080"]}}', [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects a non numeric port as malformed', () => {
      setCommandAndPorts("serve --port {{PORT['http']}}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects an unterminated candidate', () => {
      setCommandAndPorts('serve --port {{PORT[8080', [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects a candidate missing one closing brace', () => {
      setCommandAndPorts("serve --port {{PORT['8080']}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects a bare opening candidate', () => {
      setCommandAndPorts('serve --port {{PORT[', [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects a malformed candidate that follows a valid one', () => {
      setCommandAndPorts("{{PORT['8080']}} {{PORT[22", [8080, 22]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects a port written with a leading zero', () => {
      setCommandAndPorts("serve --port {{PORT['08080']}}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects a zero port', () => {
      setCommandAndPorts("serve --port {{PORT['0']}}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('treats the highest valid port as well formed', () => {
      setCommandAndPorts("serve --port {{PORT['65535']}}", [65535]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeFalse();
      expect(component.courseTechnicalForm.hasError('unknownCommandPorts')).toBeFalse();
    });

    it('rejects a port above the range', () => {
      setCommandAndPorts("serve --port {{PORT['65536']}}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects a value that wraps a uint32', () => {
      setCommandAndPorts("serve --port {{PORT['4294975376']}}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('rejects a nested candidate', () => {
      setCommandAndPorts("{{PORT[{{PORT['8080']}}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
    });

    it('reports malformed placeholders before unknown ports', () => {
      setCommandAndPorts("{{PORT[8080]}} {{PORT['9999']}}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
      expect(component.courseTechnicalForm.hasError('unknownCommandPorts')).toBeFalse();
    });
  });
});

// save_student_work_enabled retires the rsync save and the submission it fed. It must not reach
// the work volume: the volume is a plain per-environment mount, configured through the same
// checkbox, and gating it here once left every platform unable to set one up at all.
describe('CourseTechnicalInformationFormComponent work volume gating', () => {
  async function renderWithSaveEnabled(
    saveStudentWorkEnabled: boolean
  ): Promise<ComponentFixture<CourseTechnicalInformationFormComponent>> {
    await TestBed.configureTestingModule({
      declarations: [ CourseTechnicalInformationFormComponent ],
      imports: [
        TranslateTestingModule,
        AdminCourseModule,
        NoopAnimationsModule,
        HttpClientTestingModule,
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: { save_student_work_enabled: saveStudentWorkEnabled } as IAppConfig,
        },
      ],
    })
    .compileComponents();

    const fixture = TestBed.createComponent(CourseTechnicalInformationFormComponent);
    // The size and path fields only appear once the volume itself is asked for.
    fixture.componentInstance.courseTechnicalForm.get('saveStudentWork')?.setValue(true);
    fixture.detectChanges();
    return fixture;
  }

  const volumeFields = (fixture: ComponentFixture<CourseTechnicalInformationFormComponent>) => ({
    size: fixture.nativeElement.querySelector('input[formcontrolname="workdirSize"]'),
    path: fixture.nativeElement.querySelector('input[formcontrolname="workdirPath"]'),
    submit: fixture.nativeElement.querySelector('mat-checkbox[formcontrolname="allowStudentToSubmit"]'),
  });

  it('offers the volume size and path when saving is disabled', async () => {
    const fields = volumeFields(await renderWithSaveEnabled(false));

    expect(fields.size).toBeTruthy();
    expect(fields.path).toBeTruthy();
  });

  it('offers the volume size and path when saving is enabled', async () => {
    const fields = volumeFields(await renderWithSaveEnabled(true));

    expect(fields.size).toBeTruthy();
    expect(fields.path).toBeTruthy();
  });

  it('withholds the submission checkbox when saving is disabled', async () => {
    expect(volumeFields(await renderWithSaveEnabled(false)).submit).toBeNull();
  });

  it('offers the submission checkbox when saving is enabled', async () => {
    expect(volumeFields(await renderWithSaveEnabled(true)).submit).toBeTruthy();
  });
});
