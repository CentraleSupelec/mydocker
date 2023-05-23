import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ComputeTypeListComponent } from './compute-type-list.component';
import { ComputeTypeModule } from '../../compute-type.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { APP_CONFIG } from '../../../../app-config';

describe('ComputeTypeListComponent', () => {
  let component: ComputeTypeListComponent;
  let fixture: ComponentFixture<ComputeTypeListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {}
      }],
      declarations: [ ComputeTypeListComponent ],
      imports: [
        ComputeTypeModule,
        HttpClientTestingModule,
        RouterTestingModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ComputeTypeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
