import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule } from "@angular/material/icon";
import { MatTooltipModule } from "@angular/material/tooltip";
import { CourseIconStatusComponent } from "./course-icon-status.component";
import { CourseStatus } from "../../interfaces/course";


describe('CourseIconStatusComponent', () => {
  let component: CourseIconStatusComponent;
  let fixture: ComponentFixture<CourseIconStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CourseIconStatusComponent ],
      imports: [ MatIconModule, MatTooltipModule ],
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

  it('shows the delete icon for a deleted course', () => {
    component.status = CourseStatus.DELETED;
    fixture.detectChanges();

    const icons: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('mat-icon'));
    expect(icons.length).toBe(1);
    expect(icons[0].textContent?.trim()).toBe('delete');
  });
});
