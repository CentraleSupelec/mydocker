import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SessionsFormComponent } from './sessions-form.component';
import { SessionsFormModule } from "../../sessions-form.module";
import { NgxPermissionsModule } from "ngx-permissions";
import { TranslateFrenchTestingModule, TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('SessionsFormComponent', () => {
  let component: SessionsFormComponent;
  let fixture: ComponentFixture<SessionsFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SessionsFormComponent ],
      imports: [
        TranslateTestingModule,
        SessionsFormModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SessionsFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

describe('SessionsFormComponent mail links in French', () => {
  let component: SessionsFormComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SessionsFormComponent ],
      imports: [
        TranslateFrenchTestingModule,
        SessionsFormModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
    component = TestBed.createComponent(SessionsFormComponent).componentInstance;
  });

  const decoded = (link: string, field: string) =>
    new URLSearchParams(link.split('?')[1]).get(field) ?? '';

  it('uses the session change subject and body, not the deployment ones', () => {
    const link = component.askSessionChangeEmailLink;
    expect(decoded(link, 'subject')).toBe('[MyDocker] Modification de session');
    expect(decoded(link, 'body')).toContain('Je souhaite modifier les sessions');
    expect(link).not.toContain('admin.courses');
  });

  it('keeps the deployment request subject', () => {
    expect(decoded(component.askDeploymentEmailLink, 'subject')).toBe('[MyDocker] Programmation de déploiements');
  });
});
