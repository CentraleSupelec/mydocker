import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClusterNewComponent } from './cluster-new.component';
import { ClusterModule } from '../../cluster.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { APP_CONFIG } from '../../../../app-config';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('ClusterNewComponent', () => {
  let component: ClusterNewComponent;
  let fixture: ComponentFixture<ClusterNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {
          default_storage_backend: 'RBD',
        }
      }],
      declarations: [ ClusterNewComponent ],
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
    fixture = TestBed.createComponent(ClusterNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
