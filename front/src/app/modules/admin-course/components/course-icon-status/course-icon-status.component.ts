import { Component, Input } from '@angular/core';
import { CourseStatus } from "../../interfaces/course";


@Component({
  selector: 'app-course-icon-status',
  templateUrl: './course-icon-status.component.html',
  styleUrls: ['./course-icon-status.component.css']
})
export class CourseIconStatusComponent {
  @Input() status: CourseStatus | null = null;
  @Input() testToolTip: string = 'En test';
  @Input() draftToolTip: string = 'Brouillon';
  @Input() readyToolTip: string = 'Prêt';
  @Input() archivedToolTip: string = 'Archivé';

  constructor() { }
}
