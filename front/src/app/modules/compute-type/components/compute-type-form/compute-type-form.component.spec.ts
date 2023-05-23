import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ComputeTypeFormComponent } from './compute-type-form.component';
import { ComputeTypeModule } from '../../compute-type.module';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ComputeTypeFormComponent', () => {
  let component: ComputeTypeFormComponent;
  let fixture: ComponentFixture<ComputeTypeFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ComputeTypeFormComponent ],
      imports: [
        ComputeTypeModule,
        NoopAnimationsModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ComputeTypeFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
