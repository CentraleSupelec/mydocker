import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from "@angular/router";
import { map } from "rxjs/operators";
import { mergeMap } from "rxjs/operators";
import { UserCourseApiService } from "../../services/user-course-api.service";

@Component({
  selector: 'app-course-join',
  templateUrl: './course-join.component.html',
  styleUrls: ['./course-join.component.css']
})
export class CourseJoinComponent implements OnInit {

  constructor(
    private readonly route: ActivatedRoute,
    private readonly courseApiService: UserCourseApiService,
    private readonly router: Router,
  ) { }

  ngOnInit(): void {
    const splitUrl = this.router.url.split('/user-redirect/')
    const userRedirect = splitUrl.length > 1? splitUrl[1]: null;

    this.route.paramMap.pipe(
      map(
        (params) => params.get('link')
      ),
      mergeMap(
        link => this.courseApiService.getCourseInformationByLink(link)
      ),
      mergeMap(
        course => this.courseApiService.joinCourse(course.id)
      )
    ).subscribe(
      course => this.router.navigate(["/"], {queryParams: {course_id: course.id, user_redirect: userRedirect}, replaceUrl: true}),
    );
  }
}
