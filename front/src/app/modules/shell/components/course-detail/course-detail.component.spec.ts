import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CourseDetailComponent } from "./course-detail.component";
import { RouterTestingModule } from "@angular/router/testing";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";


describe('CourseDetailComponent', () => {
  let component: CourseDetailComponent;
  let fixture: ComponentFixture<CourseDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseDetailComponent ],
      imports: [
        RouterTestingModule,
        HttpClientTestingModule,
        MatProgressSpinnerModule,
      ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
