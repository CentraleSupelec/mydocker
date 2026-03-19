import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentEditComponent } from './content-edit.component';
import { ContentModule } from '../../content.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { APP_CONFIG } from '../../../../app-config';

describe('ContentEditComponent', () => {
  let component: ContentEditComponent;
  let fixture: ComponentFixture<ContentEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [{
        provide: APP_CONFIG,
        useValue: {
          default_storage_backend: 'RBD',
        }
      }],
      declarations: [ ContentEditComponent ],
      imports: [
        ContentModule,
        HttpClientTestingModule,
        RouterTestingModule,
        NoopAnimationsModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ContentEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
