import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SnackNotificationComponent } from './snack-notification.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_SNACK_BAR_DATA } from '@angular/material/snack-bar';
import { SnackNotificationModule } from "../snack-notification.module";
import { ISnackData } from "../toast";

describe('SnackNotificationComponent', () => {
  let component: SnackNotificationComponent;
  let fixture: ComponentFixture<SnackNotificationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        SnackNotificationModule,
        NoopAnimationsModule,
      ],
      providers: [
        {
          provide: MAT_SNACK_BAR_DATA,
          useValue: {
            message: 'Displayed message',
            type: 'error',
          } as ISnackData,
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SnackNotificationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
