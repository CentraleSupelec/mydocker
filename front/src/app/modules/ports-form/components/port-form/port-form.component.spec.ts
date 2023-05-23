import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PortFormComponent } from './port-form.component';
import { PortsFormModule } from "../../ports-form.module";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";

describe('PortFormComponent', () => {
  let component: PortFormComponent;
  let fixture: ComponentFixture<PortFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PortFormComponent ],
      imports: [
        PortsFormModule,
        NoopAnimationsModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PortFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
