import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ClusterListComponent } from './cluster-list.component';
import { ClusterModule } from '../../cluster.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { APP_CONFIG } from '../../../../app-config';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('ClusterListComponent', () => {
  let component: ClusterListComponent;
  let fixture: ComponentFixture<ClusterListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {}
      }],
      declarations: [ ClusterListComponent ],
      imports: [
        ClusterModule,
        HttpClientTestingModule,
        RouterTestingModule,
        TranslateTestingModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ClusterListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
