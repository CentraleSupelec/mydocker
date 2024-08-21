import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoginAcceptComponent } from './login-accept.component';
import { ActivatedRoute, convertToParamMap } from "@angular/router";
import { of } from "rxjs";
import { RouterTestingModule } from "@angular/router/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { APP_CONFIG } from "../../../../app-config";
import { NgxPermissionsModule } from "ngx-permissions";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { AuthModule } from "angular-auth-oidc-client";

describe('LoginAcceptComponent', () => {
  let component: LoginAcceptComponent;
  let fixture: ComponentFixture<LoginAcceptComponent>;

  beforeEach(async () => {
    const queryParamMap = of(convertToParamMap({ redirectTo: "/", ticket: "CAS_TOKEN" }))
    await TestBed.configureTestingModule({
      declarations: [ LoginAcceptComponent ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: queryParamMap
          }
        },
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ],
      imports: [
        RouterTestingModule,
        HttpClientTestingModule,
        NgxPermissionsModule.forRoot(),
        MatProgressSpinnerModule,
        AuthModule.forRoot({}),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(LoginAcceptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
