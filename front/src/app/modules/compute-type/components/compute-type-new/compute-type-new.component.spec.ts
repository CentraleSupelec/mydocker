import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ComputeTypeNewComponent } from './compute-type-new.component';
import { ComputeTypeModule } from '../../compute-type.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { APP_CONFIG } from '../../../../app-config';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ComputeTypeNewComponent', () => {
  let component: ComputeTypeNewComponent;
  let fixture: ComponentFixture<ComputeTypeNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {}
      }],
      declarations: [ ComputeTypeNewComponent ],
      imports: [
        ComputeTypeModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ComputeTypeNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
