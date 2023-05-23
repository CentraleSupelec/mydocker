import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CourseJoinComponent } from './course-join.component';
import { RouterTestingModule } from "@angular/router/testing";
import { APP_CONFIG } from "../../../../app-config";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";

describe('CourseJoinComponent', () => {
  let component: CourseJoinComponent;
  let fixture: ComponentFixture<CourseJoinComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseJoinComponent ],
      imports: [
        RouterTestingModule,
        HttpClientTestingModule,
        MatProgressSpinnerModule,
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {}
        }
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseJoinComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
