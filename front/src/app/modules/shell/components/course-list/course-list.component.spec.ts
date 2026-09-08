import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseListComponent } from './course-list.component';
import { RouterTestingModule } from "@angular/router/testing";
import { ActivatedRoute, convertToParamMap } from "@angular/router";
import { of } from "rxjs";
import { MatExpansionModule } from "@angular/material/expansion";
import { APP_CONFIG } from 'src/app/app-config';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

describe('CourseListComponent', () => {
  let component: CourseListComponent;
  let fixture: ComponentFixture<CourseListComponent>;

  const sessions = of({ sessions: [] });
  const queryParamMap = of(convertToParamMap({}));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CourseListComponent],
      imports: [
        TranslateTestingModule,
        RouterTestingModule,
        HttpClientTestingModule,
        MatExpansionModule,
      ],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          data: sessions,
          queryParamMap: queryParamMap
        }
      },
      {
        provide: APP_CONFIG,
        useValue: {}
      }
      ]
    })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
