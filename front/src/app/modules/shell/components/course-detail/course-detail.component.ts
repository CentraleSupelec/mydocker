import { Component, Input } from '@angular/core';
import { IBasicCourse } from "../../interfaces/course";
import { ISession } from "../../interfaces/session";

@Component({
  selector: 'app-course-detail',
  templateUrl: './course-detail.component.html',
  styleUrls: ['./course-detail.component.css']
})
export class CourseDetailComponent {
  @Input() session: ISession | null = null;
  @Input() course: IBasicCourse | undefined = undefined;
  @Input() launch: boolean = false;
  @Input() userRedirect: string | undefined = undefined;
}
