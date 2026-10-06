import { ComponentFixture, fakeAsync, flush, TestBed } from '@angular/core/testing';

import { LoginAcceptComponent } from './login-accept.component';
import { ActivatedRoute, convertToParamMap, Params, Router } from "@angular/router";
import { of, throwError } from "rxjs";
import { RouterTestingModule } from "@angular/router/testing";
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../../app-config";
import { NgxPermissionsModule } from "ngx-permissions";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { AuthModule, LoginResponse, OidcSecurityService } from "angular-auth-oidc-client";
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { TranslateFrenchTestingModule } from "src/testing/translate-testing.module";

describe('LoginAcceptComponent', () => {
  let component: LoginAcceptComponent;
  let fixture: ComponentFixture<LoginAcceptComponent>;
  let queryParams: Params;
  let config: { back_url: string, front_url: string, support_email?: string };
  let router: Router;
  let httpTestingController: HttpTestingController;

  beforeEach(async () => {
    queryParams = { redirectTo: "/", ticket: "CAS_TOKEN" };
    config = { back_url: 'http://back/', front_url: 'http://front', support_email: 'help@example.org' };
    await TestBed.configureTestingModule({
      declarations: [ LoginAcceptComponent ],
      providers: [
        {
          provide: ActivatedRoute,
          useFactory: () => ({ queryParamMap: of(convertToParamMap(queryParams)) })
        },
        {
          provide: APP_CONFIG,
          useFactory: () => config
        }
      ],
      imports: [
        RouterTestingModule,
        HttpClientTestingModule,
        NgxPermissionsModule.forRoot(),
        MatProgressSpinnerModule,
        AuthModule.forRoot({}),
        MatSnackBarModule,
        TranslateFrenchTestingModule,
      ]
    })
    .compileComponents();
  });

  function createComponent(): void {
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(LoginAcceptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function stubCheckAuth(loginResponse: Partial<LoginResponse>): void {
    spyOn(TestBed.inject(OidcSecurityService), 'checkAuth').and.returnValue(of(loginResponse as LoginResponse));
  }

  function expectSignInErrorNavigation(): string {
    expect(router.navigate).toHaveBeenCalledWith(['/login'], jasmine.objectContaining({
      queryParams: { preventAutoSignIn: 'true' },
    }));
    const extras = (router.navigate as jasmine.Spy).calls.mostRecent().args[1];
    return extras.state.error_message;
  }

  afterEach(() => {
    httpTestingController?.verify();
  });

  it('should create', () => {
    createComponent();
    httpTestingController.expectOne(req => req.url.startsWith('http://back/auth/cas/'));
    expect(component).toBeTruthy();
  });

  it('should send the user back to the login page with the support address when the CAS ticket is refused with 500', () => {
    createComponent();

    httpTestingController.expectOne(req => req.url.startsWith('http://back/auth/cas/CAS_TOKEN'))
      .flush('error', { status: 500, statusText: 'Internal Server Error' });

    expect(expectSignInErrorNavigation()).toContain('help@example.org');
  });

  it('should send the user back to the login page with the support address when the CAS ticket is refused with 401', () => {
    createComponent();

    httpTestingController.expectOne(req => req.url.startsWith('http://back/auth/cas/CAS_TOKEN'))
      .flush('error', { status: 401, statusText: 'Unauthorized' });

    expect(expectSignInErrorNavigation()).toContain('help@example.org');
  });

  it('should send the user back to the login page with the support address when the OIDC token is refused', () => {
    queryParams = { code: "OIDC_CODE" };
    stubCheckAuth({ isAuthenticated: true, accessToken: 'ACCESS_TOKEN' });
    createComponent();

    const req = httpTestingController.expectOne('http://back/auth/oidc/');
    expect(req.request.method).toBe('POST');
    req.flush('error', { status: 500, statusText: 'Internal Server Error' });

    expect(expectSignInErrorNavigation()).toContain('help@example.org');
  });

  it('should send the user back to the login page with the support address when OIDC reports not authenticated', () => {
    queryParams = { code: "OIDC_CODE" };
    stubCheckAuth({ isAuthenticated: false, errorMessage: 'denied' });
    createComponent();

    expect(expectSignInErrorNavigation()).toContain('help@example.org');
  });

  it('should send the user back to the login page with the support address when the OIDC check errors out', () => {
    queryParams = { code: "OIDC_CODE" };
    spyOn(TestBed.inject(OidcSecurityService), 'checkAuth').and.returnValue(throwError(() => new Error('no state')));
    createComponent();

    expect(expectSignInErrorNavigation()).toContain('help@example.org');
  });

  it('should keep the expired link message when the magic link is refused', fakeAsync(() => {
    queryParams = { token: "MAGIC_TOKEN" };
    createComponent();

    const req = httpTestingController.expectOne('http://back/auth/magic-link/login');
    expect(req.request.method).toBe('POST');
    req.flush('error', { status: 400, statusText: 'Bad Request' });
    // The magic-link path rethrows to an unhandled subscription, which rxjs reports from a timer
    expect(() => flush()).toThrowError();

    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      state: { error_message: 'Lien expiré' }
    });
  }));

  it('should show the message without address when no support address is configured', () => {
    delete config.support_email;
    createComponent();

    httpTestingController.expectOne(req => req.url.startsWith('http://back/auth/cas/CAS_TOKEN'))
      .flush('error', { status: 500, statusText: 'Internal Server Error' });

    expect(expectSignInErrorNavigation())
      .toBe('La connexion a échoué. Merci de contacter le support informatique.');
  });
});
