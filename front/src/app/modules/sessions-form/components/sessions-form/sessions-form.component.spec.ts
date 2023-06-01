import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SessionsFormComponent } from './sessions-form.component';
import { SessionsFormModule } from "../../sessions-form.module";
import { NgxPermissionsModule } from "ngx-permissions";

describe('SessionsFormComponent', () => {
  let component: SessionsFormComponent;
  let fixture: ComponentFixture<SessionsFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SessionsFormComponent ],
      imports: [
        SessionsFormModule,
        NgxPermissionsModule.forRoot(),
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SessionsFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
