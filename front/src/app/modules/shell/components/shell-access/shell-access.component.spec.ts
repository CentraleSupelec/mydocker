import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShellAccessComponent } from './shell-access.component';
import { APP_CONFIG } from "../../../../app-config";
import { ShellModule } from "../../shell.module";
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { NgxPermissionsModule } from "ngx-permissions";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { SimpleChange } from '@angular/core';
import { ISession } from '../../interfaces/session';
import { IBasicCourse } from '../../interfaces/course';
import { IContainer } from '../../interfaces/container';

const CONTAINER_URL = 'http://back/docker/container/42';
const INIT_URL = 'http://back/docker/initGetContainer/7';

/** The creation request carries query parameters, so it is matched on its path alone. */
function initRequest(request: { url: string }): boolean {
  return request.url === INIT_URL;
}

function startedSession(): ISession {
  return {
    id: 7,
    startDateTime: new Date().getTime() - 60000,
    blockContainerCreationBeforeStartTime: false,
    course: { id: 42 } as IBasicCourse,
  } as ISession;
}

function runningContainer(): IContainer {
  return { status: 'OK', state: 'RUNNING', ports: [] } as unknown as IContainer;
}

describe('ShellAccessComponent', () => {
  let component: ShellAccessComponent;
  let fixture: ComponentFixture<ShellAccessComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ShellAccessComponent ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: { back_url: 'http://back/' }
        },
      ],
      imports: [
        TranslateTestingModule,
        ShellModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ShellAccessComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('displays an environment observed running without asking for one', () => {
    component.session = startedSession();
    component.active = true;
    component.ngOnChanges({ active: new SimpleChange(false, true, false) });

    httpMock.expectNone(initRequest);
    const request = httpMock.expectOne(CONTAINER_URL);
    expect(request.request.method).toBe('GET');
    request.flush(runningContainer());

    expect(component.state).toBe('container_created');
    expect(component.userStarted).toBeFalse();
  });

  it('schedules the shutdown warning on the display path too', () => {
    const warning = spyOn<any>(component, 'setWarningTimeout');
    component.session = startedSession();
    component.active = true;
    component.ngOnChanges({ active: new SimpleChange(false, true, false) });

    httpMock.expectOne(CONTAINER_URL).flush(runningContainer());

    expect(warning).toHaveBeenCalled();
  });

  it('stays in the ask state when the read-only fetch finds no container', () => {
    component.session = startedSession();
    component.active = true;
    component.ngOnChanges({ active: new SimpleChange(false, true, false) });

    httpMock.expectOne(CONTAINER_URL).flush(null);

    expect(component.state).toBe('ask');
    expect(component.container).toBeNull();
  });

  it('does not ask for an environment nobody asked for and nothing reports running', () => {
    component.session = startedSession();
    component.ngOnChanges({ active: new SimpleChange(true, false, false) });

    httpMock.expectNone(CONTAINER_URL);
    httpMock.expectNone(initRequest);
  });

  it('does not post an init request for an environment already running', () => {
    component.session = startedSession();
    component.active = true;
    component.intent = 'start';
    component.ngOnChanges({
      intent: new SimpleChange('none', 'start', false),
      active: new SimpleChange(false, true, false),
    });

    httpMock.expectNone(initRequest);
    httpMock.expectOne(CONTAINER_URL).flush(runningContainer());

    expect(component.state).toBe('container_created');
    expect(component.userStarted).toBeTrue();
  });

  it('posts an init request when a start was asked for a stopped environment', () => {
    component.session = startedSession();
    component.intent = 'start';
    component.ngOnChanges({ intent: new SimpleChange('none', 'start', false) });

    const request = httpMock.expectOne(initRequest);
    expect(request.request.method).toBe('POST');
    expect(component.userStarted).toBeTrue();
  });

  it('requires a fresh start once the environment has stopped', () => {
    component.session = startedSession();
    component.active = true;
    component.intent = 'start';
    component.ngOnChanges({
      intent: new SimpleChange('none', 'start', false),
      active: new SimpleChange(false, true, false),
    });
    httpMock.expectOne(CONTAINER_URL).flush(runningContainer());

    component.active = false;
    component.ngOnChanges({ active: new SimpleChange(true, false, false) });

    expect(component.state).toBe('ask');
    expect(component.container).toBeNull();
    expect(component.userStarted).toBeFalse();
  });
});
