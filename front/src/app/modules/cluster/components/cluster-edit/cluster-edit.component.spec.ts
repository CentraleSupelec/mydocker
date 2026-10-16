import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClusterEditComponent } from './cluster-edit.component';
import { ClusterModule } from '../../cluster.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { APP_CONFIG } from '../../../../app-config';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('ClusterEditComponent', () => {
  let component: ClusterEditComponent;
  let fixture: ComponentFixture<ClusterEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {
          default_storage_backend: 'RBD',
        }
      }],
      declarations: [ ClusterEditComponent ],
      imports: [
        ClusterModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
        TranslateTestingModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ClusterEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
