import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SignInComponent } from './sign-in.component';
import { APP_CONFIG } from "../../../../app-config";
import { ActivatedRoute, convertToParamMap } from "@angular/router";
import { of } from "rxjs";

describe('SignInComponent', () => {
  let component: SignInComponent;
  let fixture: ComponentFixture<SignInComponent>;

  beforeEach(async () => {
    const queryParamMap = of(convertToParamMap({}))
    await TestBed.configureTestingModule({
      declarations: [ SignInComponent ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {},
        },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: queryParamMap
          }
        },
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SignInComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
