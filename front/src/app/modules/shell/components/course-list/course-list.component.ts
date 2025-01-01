import { AfterViewInit, Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ISession } from "../../interfaces/session";
import { IBasicCourseWithSession } from "../../interfaces/course";
import { FormControl } from "@angular/forms";
import { filter, map, mergeMap, switchMap, take, tap } from "rxjs/operators";
import { AdminCoursesApiService } from "src/app/modules/admin-course/services/admin-courses-api.service";
import { ComputeTypesApiService } from "src/app/modules/compute-type/services/compute-types-api.service";


@Component({
  selector: 'app-course-list',
  templateUrl: './course-list.component.html',
  styleUrls: ['./course-list.component.css']
})
export class CourseListComponent implements OnInit, AfterViewInit {
  selectSessionId: number | null = null;
  sessionByDate: {[date: number]: ISession[]} = {};
  courses: IBasicCourseWithSession[] = [];
  selectedTab = new FormControl(0);
  launchSessionId: number | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly adminCoursesApiService: AdminCoursesApiService,
    private readonly computeTypesApiService: ComputeTypesApiService,
  ) {}

  ngOnInit(): void {
    this.route.data
    .pipe(
      mergeMap((routeData) => {
        this.courses = routeData.courses?.map(((course: IBasicCourseWithSession) => {
          const futureSessions = course.sessions.filter(session => session.endDateTime > Date.now());
          course.sessions = futureSessions.length > 0? futureSessions: course.sessions.slice(-1)
          return course;
        }));

        routeData.sessions.forEach(
          (session: ISession) => {
            const date = new Date(session.startDateTime);
            date.setHours(0, 0, 0);
            const timestamp = date.getTime();
            if (timestamp in this.sessionByDate) {
              this.sessionByDate[timestamp].push(session)
            } else {
              this.sessionByDate[timestamp] = [session]
            }
          });
        return this.route.queryParamMap;
      }),
      take(1),
      map((queryParamMap) => {
        if (queryParamMap.has("course_id")) {
          const courseId = parseInt(<string>queryParamMap.get("course_id"));
          const sessionId = this.courses.find((course) => course.id === courseId)?.sessions[0].id;
          if (sessionId) {
            this.selectSessionId = sessionId;
            this.adminCoursesApiService.getCourse(courseId).pipe(
              switchMap(course => this.computeTypesApiService.getComputeType(course.computeTypeId)),
              filter(computeType => !computeType.gpu),
              tap(() => this.launchSessionId = sessionId)
            ).subscribe();
          }
        }
        if (Object.keys(this.sessionByDate).length === 0 || queryParamMap.has("course_id")) {
          this.selectedTab.setValue(1);
        }
      })
    ).subscribe();
    this.route.queryParamMap.subscribe(
      queryParamMap => {
        if(queryParamMap.has('session_id')) {
          this.selectSessionId = parseInt(<string>queryParamMap.get('session_id'));
        }
      }
    )
  }

  ngAfterViewInit() {
    if (!this.selectSessionId) {
      return;
    }
    // mandatory to compute the right offset only when all accordions are initialized (and closed)
    setTimeout(() => {
      document.getElementById(this.getSessionHtmlId(
        this.selectedTab.value, this.selectSessionId
      ))?.scrollIntoView();
    }, 0);
  }

  afterExpand(element: ISession) {
    this.selectSessionId = element.id;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        session_id: element.id,
        course_id: null,
      },
      queryParamsHandling: "merge"
    });
  }

  afterCollapse(element: ISession) {
    if (this.selectSessionId === element.id) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {
          session_id: null,
          course_id: null,
        },
        queryParamsHandling: "merge"
      });
      this.selectSessionId = null;
    }
  }

  getSessionHtmlId(tab: number, sessionId: number | null): string {
    return `session-${tab}-${sessionId}`;
  }
}
