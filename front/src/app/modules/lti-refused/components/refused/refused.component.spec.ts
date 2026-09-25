import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';

import { RefusedComponent } from './refused.component';
import { APP_CONFIG } from '../../../../app-config';

describe('RefusedComponent', () => {
  let fixture: ComponentFixture<RefusedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RefusedComponent ],
      imports: [ TranslateModule.forRoot() ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: { app_name: 'MyDocker' },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(RefusedComponent);
    fixture.detectChanges();
  });

  it('links to the application root in a new tab', () => {
    const link: HTMLAnchorElement = fixture.nativeElement.querySelector('a');
    expect(link.getAttribute('href')).toBe('/');
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toBe('noopener');
  });

  it('falls back to MyDocker when no application name is configured', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      declarations: [ RefusedComponent ],
      imports: [ TranslateModule.forRoot() ],
      providers: [ { provide: APP_CONFIG, useValue: {} } ],
    });
    expect(TestBed.createComponent(RefusedComponent).componentInstance.appName).toBe('MyDocker');
  });
});
