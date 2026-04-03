import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentNewComponent } from './content-new.component';
import { ContentModule } from '../../content.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { APP_CONFIG } from '../../../../app-config';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ContentNewComponent', () => {
  let component: ContentNewComponent;
  let fixture: ComponentFixture<ContentNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {
          default_storage_backend: 'RBD',
        }
      }],
      declarations: [ ContentNewComponent ],
      imports: [
        ContentModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ContentNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
