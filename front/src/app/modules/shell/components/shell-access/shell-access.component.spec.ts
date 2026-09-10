import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { ShellAccessComponent } from './shell-access.component';
import { APP_CONFIG } from "../../../../app-config";
import { ShellModule } from "../../shell.module";
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { NgxPermissionsModule } from "ngx-permissions";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { SimpleChange } from '@angular/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ISession } from '../../interfaces/session';
import { IBasicCourse } from '../../interfaces/course';
import { IContainer } from '../../interfaces/container';
import { SnackNotificationService } from '../../../utils/snack-notification/snack-notification.service';
import { MAX_STATUS_ATTEMPTS } from './next-retry-decision';

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

function startingContainer(): IContainer {
  return { status: 'PENDING', state: 'STARTING', ports: [] } as unknown as IContainer;
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

  it('spends the start when the environment stops while the read is in flight', () => {
    component.session = startedSession();
    component.active = true;
    component.intent = 'start';
    component.ngOnChanges({
      intent: new SimpleChange('none', 'start', false),
      active: new SimpleChange(false, true, false),
    });
    const request = httpMock.expectOne(CONTAINER_URL);

    component.active = false;
    component.ngOnChanges({ active: new SimpleChange(true, false, false) });
    request.flush(runningContainer());

    expect(component.userStarted).toBeFalse();
    expect(component.state).toBe('ask');
    expect(component.container).toBeNull();
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

  describe('when the status of a starting environment cannot be read', () => {
    let snack: jasmine.Spy;

    function startCreation(): void {
      component.session = startedSession();
      component.intent = 'start';
      component.ngOnChanges({ intent: new SimpleChange('none', 'start', false) });
      httpMock.expectOne(initRequest).flush(null);
    }

    function failNextPoll(): string {
      tick(3000);
      const pending = httpMock.match(() => true);
      expect(pending.length).toBe(1);
      const method = pending[0].request.method;
      pending[0].flush('', { status: 503, statusText: 'Service Unavailable' });
      return method;
    }

    beforeEach(() => {
      snack = spyOn(TestBed.inject(SnackNotificationService), 'push');
    });

    it('gives up after five failures, notifies once and offers a retry', fakeAsync(() => {
      startCreation();

      const methods: string[] = [];
      for (let attempt = 0; attempt < MAX_STATUS_ATTEMPTS; attempt++) {
        methods.push(failNextPoll());
      }

      expect(methods.filter((method) => 'POST' === method).length).toBe(1);
      expect(component.state).toBe('unavailable');
      expect(snack).toHaveBeenCalledTimes(1);

      tick(3000);
      httpMock.expectNone(() => true);

      discardPeriodicTasks();
    }));

    it('stops at once, without recovering, when the back end refuses the read', fakeAsync(() => {
      startCreation();

      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush('', { status: 401, statusText: 'Unauthorized' });

      expect(component.state).toBe('unavailable');
      expect(snack).toHaveBeenCalledTimes(1);

      tick(3000);
      httpMock.expectNone(() => true);

      discardPeriodicTasks();
    }));

    it('never posts a creation request while it is only watching', fakeAsync(() => {
      component.session = startedSession();
      component.active = true;
      component.ngOnChanges({ active: new SimpleChange(false, true, false) });
      httpMock.expectOne(CONTAINER_URL).flush(startingContainer());

      const methods: string[] = [];
      for (let attempt = 0; attempt < MAX_STATUS_ATTEMPTS; attempt++) {
        methods.push(failNextPoll());
      }

      expect(methods.filter((method) => 'POST' === method).length).toBe(0);
      expect(component.state).toBe('unavailable');

      discardPeriodicTasks();
    }));

    it('forgets earlier failures once a read succeeds', fakeAsync(() => {
      startCreation();

      failNextPoll();

      tick(3000);
      httpMock.expectOne(initRequest).flush(null);
      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush(startingContainer());

      for (let attempt = 0; attempt < MAX_STATUS_ATTEMPTS - 1; attempt++) {
        failNextPoll();
      }

      expect(component.state).toBe('pending');
      expect(snack).not.toHaveBeenCalled();

      discardPeriodicTasks();
    }));
  });
});

// The rsync save is retired on every platform. Its button used to be gated on the course fields
// alone, so a course still carrying them offered a button the back end answers with 403.
describe('ShellAccessComponent save button gating', () => {
  function submittableSession(): ISession {
    return {
      id: 7,
      startDateTime: new Date().getTime() - 60000,
      blockContainerCreationBeforeStartTime: false,
      course: { id: 42, allowStudentToSubmit: true, studentWorkIsSaved: true } as IBasicCourse,
    } as ISession;
  }

  async function renderRunningWithSaveEnabled(
    saveStudentWorkEnabled: boolean
  ): Promise<ComponentFixture<ShellAccessComponent>> {
    await TestBed.configureTestingModule({
      declarations: [ ShellAccessComponent ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: { back_url: 'http://back/', save_student_work_enabled: saveStudentWorkEnabled },
        },
      ],
      imports: [
        TranslateTestingModule,
        ShellModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NgxPermissionsModule.forRoot(),
        // The running panel renders material form fields, which animate.
        NoopAnimationsModule,
      ]
    })
    .compileComponents();

    const fixture = TestBed.createComponent(ShellAccessComponent);
    const component = fixture.componentInstance;
    component.session = submittableSession();
    component.active = true;
    component.ngOnChanges({ active: new SimpleChange(false, true, false) });

    TestBed.inject(HttpTestingController).expectOne(CONTAINER_URL).flush(runningContainer());
    fixture.detectChanges();
    return fixture;
  }

  it('withholds the save button on an infrastructure where saving is disabled', async () => {
    const fixture = await renderRunningWithSaveEnabled(false);

    expect(fixture.componentInstance.state).toBe('container_created');
    expect(fixture.nativeElement.querySelector('app-save-state')).toBeNull();
  });

  it('offers the save button when saving is enabled', async () => {
    const fixture = await renderRunningWithSaveEnabled(true);

    expect(fixture.nativeElement.querySelector('app-save-state')).toBeTruthy();
  });
});
