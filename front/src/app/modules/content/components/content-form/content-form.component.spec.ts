import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentFormComponent } from './content-form.component';
import { ContentModule } from '../../content.module';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('ContentFormComponent', () => {
  let component: ContentFormComponent;
  let fixture: ComponentFixture<ContentFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ContentFormComponent ],
      imports: [
        TranslateTestingModule,
        ContentModule,
        NoopAnimationsModule
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ContentFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
