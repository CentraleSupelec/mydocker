import { ComponentFixture, discardPeriodicTasks, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';

import { ShellAccessComponent } from './shell-access.component';
import { APP_CONFIG } from "../../../../app-config";
import { ShellModule } from "../../shell.module";
import { HttpClientTestingModule, HttpTestingController, TestRequest } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { NgxPermissionsModule } from "ngx-permissions";
import { TranslateService } from '@ngx-translate/core';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';
import { SimpleChange } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DisplayContainerComponent } from '../../../display-container/display-container/display-container.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ISession } from '../../interfaces/session';
import { IBasicCourse } from '../../interfaces/course';
import { IContainer } from '../../interfaces/container';
import { SnackNotificationService } from '../../../utils/snack-notification/snack-notification.service';
import { MAX_STATUS_ATTEMPTS } from './next-retry-decision';

const CONTAINER_URL = 'http://back/docker/container/42';
const INIT_URL = 'http://back/docker/initGetContainer/7';
/** Shorter than the 420 s default, so that the specs show the configured bound is the one applied. */
const START_RESPONSE_TIMEOUT_IN_SECONDS = 90;

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

/** The task runs, the back end's connection test has not passed yet. */
function untestedContainer(): IContainer {
  return { status: 'PENDING', state: 'RUNNING', ports: [] } as unknown as IContainer;
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
          useValue: { back_url: 'http://back/', start_response_timeout_in_seconds: START_RESPONSE_TIMEOUT_IN_SECONDS }
        },
      ],
      imports: [
        TranslateTestingModule,
        ShellModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NgxPermissionsModule.forRoot(),
        // Rendering the created state reaches material form fields, which use animations.
        NoopAnimationsModule,
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

  it('treats the request button as a start the student asked for', () => {
    component.session = startedSession();
    fixture.detectChanges();

    const button: HTMLButtonElement = fixture.debugElement
      .query(By.css('button[color="primary"]')).nativeElement;
    button.click();

    const request = httpMock.expectOne(initRequest);
    expect(request.request.method).toBe('POST');
    expect(component.userStarted).toBeTrue();
  });

  it('enables autoclick on the environment it was asked to create', () => {
    component.session = startedSession();

    component.initGetContainer();
    httpMock.expectOne(initRequest).flush({});
    component.container = runningContainer();
    component.state = 'container_created';
    fixture.detectChanges();

    const display = fixture.debugElement.query(By.directive(DisplayContainerComponent));
    expect(display.componentInstance.enableAutoClick).toBeTrue();
  });

  it('leaves autoclick off on an environment it only discovered running', () => {
    component.session = startedSession();
    component.active = true;
    component.ngOnChanges({ active: new SimpleChange(false, true, false) });
    httpMock.expectOne(CONTAINER_URL).flush(runningContainer());
    fixture.detectChanges();

    const display = fixture.debugElement.query(By.directive(DisplayContainerComponent));
    expect(display.componentInstance.enableAutoClick).toBeFalse();
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

    it('ends a start no record answered after five failed reads, and retries it as a start', fakeAsync(() => {
      startCreation();

      failNextPoll();
      // The one recovery request is answered; the reads that follow keep failing.
      tick(3000);
      httpMock.expectOne(initRequest).flush(null);
      for (let attempt = 1; attempt < MAX_STATUS_ATTEMPTS; attempt++) {
        expect(failNextPoll()).toBe('GET');
      }

      expect(component.startFailed).toBeTrue();
      expect(component.state).toBe('start_failed');
      expect(snack).not.toHaveBeenCalled();

      tick(3000);
      httpMock.expectNone(() => true);

      fixture.detectChanges();
      fixture.debugElement.query(By.css('button[color="primary"]'))?.nativeElement.click();
      httpMock.expectOne(initRequest).flush('', { status: 503, statusText: 'Service Unavailable' });

      discardPeriodicTasks();
      flush();
      httpMock.verify();
    }));

    it('ends a start no record answered at once when the back end refuses the read', fakeAsync(() => {
      startCreation();

      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush('', { status: 401, statusText: 'Unauthorized' });

      expect(component.startFailed).toBeTrue();
      expect(component.state).toBe('start_failed');

      tick(3000);
      httpMock.expectNone(() => true);

      discardPeriodicTasks();
      flush();
      httpMock.verify();
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
      expect(component.startFailed).toBeFalse();
      expect(snack).toHaveBeenCalledTimes(1);

      tick(3000);
      httpMock.expectNone(() => true);

      discardPeriodicTasks();
    }));

    it('forgets earlier failures once a read succeeds', fakeAsync(() => {
      startCreation();

      failNextPoll();

      tick(3000);
      httpMock.expectOne(initRequest).flush(null);
      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush(startingContainer());

      // The first failure since the record may recover again; that request is answered.
      failNextPoll();
      tick(3000);
      httpMock.expectOne(initRequest).flush(null);
      for (let attempt = 0; attempt < MAX_STATUS_ATTEMPTS - 2; attempt++) {
        failNextPoll();
      }

      expect(component.state).toBe('pending');
      expect(snack).not.toHaveBeenCalled();

      // The recovery request armed its own bound, still running here.
      discardPeriodicTasks();
      flush();
      httpMock.match(() => true);
      httpMock.verify();
    }));
  });

  // Every spec here settles its own timers and requests: on a panel that leaves a start error
  // unhandled, the error must fail the spec that caused it rather than reach the end of the run.
  describe('when a start fails or goes unanswered', () => {
    function startErrorText(): string {
      fixture.detectChanges();
      return fixture.nativeElement.querySelector('.start-error')?.textContent ?? '';
    }

    function spinner(): Element | null {
      fixture.detectChanges();
      return fixture.nativeElement.querySelector('mat-spinner');
    }

    /** A record ends the first bound, then a read fails: the next tick sends the one recovery request. */
    function sendRecovery(): TestRequest {
      component.session = startedSession();
      component.initGetContainer();
      httpMock.expectOne(initRequest).flush(null);
      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush(startingContainer());
      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush('', { status: 500, statusText: 'Internal Server Error' });
      tick(3000);
      return httpMock.expectOne(initRequest);
    }

    it('ends the loader and shows the startup error with the reason the back end gave', fakeAsync(() => {
      const instant = spyOn(TestBed.inject(TranslateService), 'instant').and.callThrough();
      component.session = startedSession();
      component.initGetContainer();

      httpMock.expectOne(initRequest).flush(
        { message: 'volume precreation failed' },
        { status: 500, statusText: 'Internal Server Error' }
      );
      flush();

      expect(component.startFailed).toBeTrue();
      expect(spinner()).toBeNull();
      expect(startErrorText()).toContain('container.impossible_to_start');
      // The translation keys are echoed here: this one is the message that carries the detail.
      expect(startErrorText()).toContain('container.problem_encountered');
      expect(instant).toHaveBeenCalledWith('container.problem_encountered', { error: 'volume precreation failed' });

      httpMock.verify();
    }));

    it('ends the start with the reason a failed recovery request gave', fakeAsync(() => {
      const instant = spyOn(TestBed.inject(TranslateService), 'instant').and.callThrough();
      component.session = startedSession();
      component.initGetContainer();
      httpMock.expectOne(initRequest).flush(null);

      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush('', { status: 500, statusText: 'Internal Server Error' });
      tick(3000);
      httpMock.expectOne(initRequest).flush(
        { message: 'volume precreation failed' },
        { status: 500, statusText: 'Internal Server Error' }
      );

      expect(component.startFailed).toBeTrue();
      expect(spinner()).toBeNull();
      expect(startErrorText()).toContain('container.problem_encountered');
      expect(instant).toHaveBeenCalledWith('container.problem_encountered', { error: 'volume precreation failed' });

      discardPeriodicTasks();
      flush();
      httpMock.verify();
    }));

    it('keeps a slow recovery request outstanding and uses its answer', fakeAsync(() => {
      const recovery = sendRecovery();

      // Several ticks pass while the request is out: none cancels it nor sends another.
      tick(10000);
      expect(recovery.cancelled).toBeFalse();
      httpMock.expectNone(() => true);

      recovery.flush(null);
      expect(component.recovering).toBeFalse();
      expect(component.startFailed).toBeFalse();

      // Polling resumes on the answer.
      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush(runningContainer());
      expect(component.state).toBe('container_created');

      discardPeriodicTasks();
      flush();
      httpMock.verify();
    }));

    it('ends the start with the reason a slow recovery request failed with', fakeAsync(() => {
      const instant = spyOn(TestBed.inject(TranslateService), 'instant').and.callThrough();
      const recovery = sendRecovery();

      tick(10000);
      expect(recovery.cancelled).toBeFalse();
      httpMock.expectNone(() => true);
      recovery.flush(
        { message: 'volume precreation failed' },
        { status: 500, statusText: 'Internal Server Error' }
      );

      expect(component.startFailed).toBeTrue();
      expect(component.startError).toBe('volume precreation failed');
      expect(startErrorText()).toContain('container.problem_encountered');
      expect(startErrorText()).not.toContain('container.unknown_error');
      expect(instant).toHaveBeenCalledWith('container.problem_encountered', { error: 'volume precreation failed' });

      // The bound went with the answer: nothing replaces the reason later.
      tick(1000 * START_RESPONSE_TIMEOUT_IN_SECONDS);
      expect(component.startError).toBe('volume precreation failed');
      httpMock.expectNone(() => true);

      discardPeriodicTasks();
      flush();
      httpMock.verify();
    }));

    it('bounds a recovery request nothing answers from the moment it is sent', fakeAsync(() => {
      const recovery = sendRecovery();

      tick(1000 * START_RESPONSE_TIMEOUT_IN_SECONDS - 1);
      expect(component.startFailed).toBeFalse();
      expect(recovery.cancelled).toBeFalse();
      httpMock.expectNone(() => true);

      tick(1);
      expect(component.startFailed).toBeTrue();
      expect(recovery.cancelled).toBeTrue();
      expect(startErrorText()).toContain('container.unknown_error');

      tick(3000);
      httpMock.expectNone(() => true);

      discardPeriodicTasks();
      flush();
      httpMock.verify();
    }));

    it('gives up on a start nothing answers once the configured bound has passed', fakeAsync(() => {
      component.session = startedSession();
      component.initGetContainer();
      const request = httpMock.expectOne(initRequest);

      expect(spinner()).not.toBeNull();
      tick(1000 * START_RESPONSE_TIMEOUT_IN_SECONDS - 1);
      expect(component.startFailed).toBeFalse();

      tick(1);
      expect(component.startFailed).toBeTrue();
      expect(request.cancelled).toBeTrue();
      expect(spinner()).toBeNull();
      expect(startErrorText()).toContain('container.impossible_to_start');
      expect(startErrorText()).toContain('container.unknown_error');

      flush();
      discardPeriodicTasks();
      httpMock.verify();
    }));

    it('retries without a reload, and a record of the environment ends the bound', fakeAsync(() => {
      component.session = startedSession();
      component.initGetContainer();
      httpMock.expectOne(initRequest).flush('', { status: 502, statusText: 'Bad Gateway' });
      flush();
      fixture.detectChanges();

      const retry = fixture.debugElement.query(By.css('button[color="primary"]'));
      expect(retry).not.toBeNull();
      retry?.nativeElement.click();

      const again = httpMock.expectOne(initRequest);
      expect(again.request.method).toBe('POST');
      expect(component.startFailed).toBeFalse();
      expect(spinner()).not.toBeNull();
      again.flush(null);

      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush(startingContainer());
      expect(component.state).toBe('pending');

      // Well past the bound, the environment still starting: the record has disarmed the timer.
      for (let elapsed = 0; elapsed <= START_RESPONSE_TIMEOUT_IN_SECONDS; elapsed += 3) {
        tick(3000);
        httpMock.expectOne(CONTAINER_URL).flush(startingContainer());
      }
      expect(component.startFailed).toBeFalse();
      expect(component.state).toBe('pending');

      discardPeriodicTasks();
      flush();
      httpMock.verify();
    }));
  });

  describe('when the task runs before the connection test passes', () => {
    function startCreation(): void {
      component.session = startedSession();
      component.intent = 'start';
      component.ngOnChanges({ intent: new SimpleChange('none', 'start', false) });
      httpMock.expectOne(initRequest).flush(null);
    }

    it('keeps polling until the status is OK', fakeAsync(() => {
      startCreation();

      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush(untestedContainer());
      expect(component.state).toBe('pending');

      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush(runningContainer());
      expect(component.state).toBe('container_created');

      tick(3000);
      httpMock.expectNone(() => true);

      discardPeriodicTasks();
    }));

    it('still stops on a failed connection test', fakeAsync(() => {
      startCreation();

      tick(3000);
      httpMock.expectOne(CONTAINER_URL).flush({ status: 'KO', state: 'RUNNING', ports: [] });
      expect(component.state).toBe('container_created');

      tick(3000);
      httpMock.expectNone(() => true);

      discardPeriodicTasks();
    }));

    it('does not show a discovered environment as created before its test passes', fakeAsync(() => {
      component.session = startedSession();
      component.active = true;
      component.ngOnChanges({ active: new SimpleChange(false, true, false) });
      httpMock.expectOne(CONTAINER_URL).flush(untestedContainer());

      expect(component.state).not.toBe('container_created');

      tick(3000);
      httpMock.expectNone(initRequest);
      httpMock.expectOne(CONTAINER_URL).flush(runningContainer());
      expect(component.state).toBe('container_created');
      expect(component.userStarted).toBeFalse();

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
