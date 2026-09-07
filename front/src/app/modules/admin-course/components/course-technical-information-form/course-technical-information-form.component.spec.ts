import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseTechnicalInformationFormComponent } from './course-technical-information-form.component';
import { AdminCourseModule } from "../../admin-course.module";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { TranslateModule } from "@ngx-translate/core";

describe('CourseTechnicalInformationFormComponent', () => {
  let component: CourseTechnicalInformationFormComponent;
  let fixture: ComponentFixture<CourseTechnicalInformationFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseTechnicalInformationFormComponent ],
      imports: [
        AdminCourseModule,
        NoopAnimationsModule,
        HttpClientTestingModule,
        // The i18n migration left this spec without a TranslateService provider, so the
        // fixture could not be created at all before this was added.
        TranslateModule.forRoot(),
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

    it('reports malformed placeholders before unknown ports', () => {
      setCommandAndPorts("{{PORT[8080]}} {{PORT['9999']}}", [8080]);
      expect(component.courseTechnicalForm.hasError('malformedCommandPorts')).toBeTrue();
      expect(component.courseTechnicalForm.hasError('unknownCommandPorts')).toBeFalse();
    });
  });
});
