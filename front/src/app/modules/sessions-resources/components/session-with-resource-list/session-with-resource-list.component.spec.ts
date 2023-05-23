import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SessionWithResourceListComponent } from './session-with-resource-list.component';
import { SessionsResourcesModule } from "../../sessions-resources.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { APP_CONFIG } from "../../../../app-config";
import { MatNativeDateModule } from "@angular/material/core";
import localeFr from '@angular/common/locales/fr';
import { registerLocaleData } from "@angular/common";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

describe('SessionWithResourceListComponent', () => {
  let component: SessionWithResourceListComponent;
  let fixture: ComponentFixture<SessionWithResourceListComponent>;

  beforeEach(async () => {
    registerLocaleData(localeFr, 'fr');
    await TestBed.configureTestingModule({
      declarations: [ SessionWithResourceListComponent ],
      imports: [
        SessionsResourcesModule,
        HttpClientTestingModule,
        RouterTestingModule,
        MatNativeDateModule,
        NoopAnimationsModule,
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SessionWithResourceListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
