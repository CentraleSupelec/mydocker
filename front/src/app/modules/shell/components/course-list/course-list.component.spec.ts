import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseListComponent } from './course-list.component';
import { RouterTestingModule } from "@angular/router/testing";
import { ActivatedRoute, convertToParamMap } from "@angular/router";
import { of } from "rxjs";
import { MatExpansionModule } from "@angular/material/expansion";

describe('CourseListComponent', () => {
  let component: CourseListComponent;
  let fixture: ComponentFixture<CourseListComponent>;

  const sessions = of({ sessions: [] });
  const queryParamMap = of(convertToParamMap({}));

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseListComponent ],
      imports: [
        RouterTestingModule,
        MatExpansionModule,
      ],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          data: sessions,
          queryParamMap: queryParamMap
        }
      }]
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
