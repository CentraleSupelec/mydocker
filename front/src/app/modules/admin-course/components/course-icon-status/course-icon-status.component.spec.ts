import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CourseIconStatusComponent } from "./course-icon-status.component";


describe('CourseIconStatusComponent', () => {
  let component: CourseIconStatusComponent;
  let fixture: ComponentFixture<CourseIconStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseIconStatusComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CourseIconStatusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
