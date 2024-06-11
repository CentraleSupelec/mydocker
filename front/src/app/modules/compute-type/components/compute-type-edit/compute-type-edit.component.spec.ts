import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ComputeTypeEditComponent } from './compute-type-edit.component';
import { ComputeTypeModule } from '../../compute-type.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { APP_CONFIG } from '../../../../app-config';

describe('ComputeTypeEditComponent', () => {
  let component: ComputeTypeEditComponent;
  let fixture: ComponentFixture<ComputeTypeEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {}
      }],
      declarations: [ ComputeTypeEditComponent ],
      imports: [
        ComputeTypeModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ComputeTypeEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
