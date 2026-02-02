import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DisplayContainerComponent } from './display-container.component';
import { DisplayContainerModule } from '../display-container.module';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('DisplayContainerComponent', () => {
  let component: DisplayContainerComponent;
  let fixture: ComponentFixture<DisplayContainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [DisplayContainerComponent],
      imports: [DisplayContainerModule, NoopAnimationsModule],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DisplayContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  // --------------------------------------------------------
  // Tests for shouldAutoclick()
  // --------------------------------------------------------

  it('returns false if more than one custom port is defined', () => {
    component.displayOptions = {
      customPortsDisplay: [{}, {}] as any[],
    } as any;
    expect(component.shouldAutoclick()).toBeFalse();
  });

  it('returns false if container adds more than one displayed port', () => {
    component.displayOptions = {
      customPortsDisplay: [],
      displayPorts: { '80': true, '443': true },
    } as any;

    component.container = {
      ports: [{ mapPort: 80 }, { mapPort: 443 }],
    } as any;

    expect(component.shouldAutoclick()).toBeFalse();
  });

  it('returns false when one custom port and one displayed container port exist', () => {
    component.displayOptions = {
      customPortsDisplay: [{ url: 'http://localhost/custom' }] as any[],
      displayUsername: false,
      displayPassword: false,
      displayPorts: { '80': true },
    } as any;
  
    component.container = {
      ports: [{ mapPort: 80 }],
    } as any;
  
    // Only one custom port + one visible container port → total 2 → should be false
    expect(component.shouldAutoclick()).toBeFalse();
  });
  

  it('returns true when one port is visible (not custom) and no credentials are displayed', () => {
    component.displayOptions = {
      customPortsDisplay: [] as any[],
      displayUsername: false,
      displayPassword: false,
    } as any;

    component.container = {
      ports: [{ mapPort: 80 }],
    } as any;

    expect(component.shouldAutoclick()).toBeTrue();
  });

  it('returns true when one port is visible (custom) and no credentials are displayed', () => {
    component.displayOptions = {
      customPortsDisplay: [{}] as any[],
      displayUsername: false,
      displayPassword: false,
    } as any;

    component.container = {
      ports: [],
    } as any;

    expect(component.shouldAutoclick()).toBeTrue();
  });

  it('returns true if URL contains {{PASSWORD}}', () => {
    component.displayOptions = {
      customPortsDisplay: [{ url: 'http://localhost/{{PASSWORD}}' }] as any[],
      displayUsername: true,
      displayPassword: true,
    } as any;

    expect(component.shouldAutoclick()).toBeTrue();
  });

  it('returns true when userRedirect is custom and URL does not container USER_REDIRECT', () => {
    component.displayOptions = {
      customPortsDisplay: [{ url: 'http://localhost/app' }] as any[],
      displayUsername: true,
      displayPassword: true,
    } as any;

    component.userRedirect = '/git_clone?repo=';

    expect(component.shouldAutoclick()).toBeFalse();
  });

  it('returns false when port URL contains USER_REDIRECT', () => {
    component.displayOptions = {
      customPortsDisplay: [{ url: 'http://localhost/USER_REDIRECT' }] as any[],
      displayUsername: true,
      displayPassword: true,
    } as any;

    component.userRedirect = '/git_clone?repo=';

    expect(component.shouldAutoclick()).toBeTrue();
  });

  it('returns false when none of the autoclick conditions are met', () => {
    component.displayOptions = {
      customPortsDisplay: [{ url: 'http://localhost/app' }] as any[],
      displayUsername: true,
      displayPassword: true,
    } as any;

    expect(component.shouldAutoclick()).toBeFalse();
  });

  // --------------------------------------------------------
  // Tests for shouldDisplay()
  // --------------------------------------------------------

  it('returns true if displayOptions or displayPorts is undefined', () => {
    expect(component.shouldDisplay({ mapPort: 80 } as any)).toBeTrue();
  });

  it('returns the value from displayPorts if defined', () => {
    component.displayOptions = { displayPorts: { '80': false } } as any;
    expect(component.shouldDisplay({ mapPort: 80 } as any)).toBeFalse();
  });

  it('returns true if mapPort not in displayPorts', () => {
    component.displayOptions = { displayPorts: { '443': false } } as any;
    expect(component.shouldDisplay({ mapPort: 80 } as any)).toBeTrue();
  });

  // --------------------------------------------------------
  // Tests for portDisplayHasNoUserRedirect()
  // --------------------------------------------------------

  it('returns false if URL contains USER_REDIRECT', () => {
    expect(
      component.portDisplayHasNoUserRedirect({ url: '/USER_REDIRECT' } as any)
    ).toBeFalse();
  });

  it('returns false if URL contains USER-REDIRECT', () => {
    expect(
      component.portDisplayHasNoUserRedirect({ url: '/USER-REDIRECT' } as any)
    ).toBeFalse();
  });

  it('returns true if URL contains neither', () => {
    expect(
      component.portDisplayHasNoUserRedirect({ url: '/app' } as any)
    ).toBeTrue();
  });

  it('is case-sensitive and returns true for lowercase user_redirect', () => {
    expect(
      component.portDisplayHasNoUserRedirect({ url: '/user_redirect' } as any)
    ).toBeTrue();
  });

});
