import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { APP_CONFIG } from 'src/app/app-config';
import { ContentShowComponent } from './content-show.component';

describe('ContentShowComponent', () => {
  let component: ContentShowComponent;
  let fixture: ComponentFixture<ContentShowComponent>;

  const data = of({ content: { richText: '<p>content</p>' } });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ContentShowComponent],
      imports: [
        RouterTestingModule,
      ],
      providers: [
        { provide: ActivatedRoute, useValue: { data } },
        { provide: APP_CONFIG, useValue: {} },
      ],
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ContentShowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
