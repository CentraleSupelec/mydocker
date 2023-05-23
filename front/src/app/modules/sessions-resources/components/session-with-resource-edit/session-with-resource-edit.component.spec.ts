import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SessionWithResourceEditComponent } from './session-with-resource-edit.component';
import { SessionsResourcesModule } from "../../sessions-resources.module";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { APP_CONFIG } from "../../../../app-config";
import { SnackNotificationModule } from "../../../utils/snack-notification/snack-notification.module";
import { ActivatedRoute } from "@angular/router";
import { of } from "rxjs";

describe('SessionWithResourceEditComponent', () => {
  let component: SessionWithResourceEditComponent;
  let fixture: ComponentFixture<SessionWithResourceEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SessionWithResourceEditComponent ],
      imports: [
        SessionsResourcesModule,
        HttpClientTestingModule,
        RouterTestingModule,
        SnackNotificationModule
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        },
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
                session: {
                  course: {},
                  resources: []
                },
                resources: []
              }
            )
          }
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SessionWithResourceEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
