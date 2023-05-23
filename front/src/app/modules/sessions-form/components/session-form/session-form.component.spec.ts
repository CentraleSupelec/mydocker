import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SessionFormComponent } from './session-form.component';
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { SessionsFormModule } from "../../sessions-form.module";
import { MatNativeDateModule } from "@angular/material/core";

describe('SessionFormComponent', () => {
  let component: SessionFormComponent;
  let fixture: ComponentFixture<SessionFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SessionFormComponent ],
      imports: [
        SessionsFormModule,
        NoopAnimationsModule,
        MatNativeDateModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SessionFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
