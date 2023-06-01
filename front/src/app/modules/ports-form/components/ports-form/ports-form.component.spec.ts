import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PortsFormComponent } from './ports-form.component';
import { PortsFormModule } from "../../ports-form.module";

describe('PortsFormComponent', () => {
  let component: PortsFormComponent;
  let fixture: ComponentFixture<PortsFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PortsFormComponent ],
      imports: [
        PortsFormModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PortsFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
