import { AfterViewInit, Component, Inject, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ISession } from "../../interfaces/session";
import { IBasicCourseWithSession } from "../../interfaces/course";
import { FormControl } from "@angular/forms";
import { filter, map, mergeMap, switchMap, take, tap } from "rxjs/operators";
import { APP_CONFIG, IAppConfig } from "src/app/app-config";
import { UserCourseApiService } from "../../services/user-course-api.service";


@Component({
  selector: 'app-course-list',
  templateUrl: './course-list.component.html',
  styleUrls: ['./course-list.component.css']
})
export class CourseListComponent implements OnInit, AfterViewInit {
  selectSessionId: number | null = null;
  planified: IBasicCourseWithSession[] = [];
  past: IBasicCourseWithSession[] = [];
  launchSessionId: number | undefined = undefined;
  courseId: number | undefined = undefined;
  userRedirect: string | undefined = undefined;
  documentationUrl: string | undefined = undefined;
  showInformationMessage: boolean = false;
  errorMessage: string | null = null;

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly userCourseApiService: UserCourseApiService,
  ) {}

  ngOnInit(): void {
    const helpInfo = this.config.information?.find((info) => info.name === "help");
    this.documentationUrl = helpInfo?.url || "https://example.com/documentation";
    this.showInformationMessage = !!helpInfo;

    this.route.data
    .pipe(
      mergeMap((routeData) => {
        this.planified = routeData.courses?.map(((course: IBasicCourseWithSession) => {
            const startOfToday = new Date()
            startOfToday.setHours(0, 0, 0, 0);

            return {
              ...course,
              sessions: course.sessions.filter(session => session.startDateTime >= startOfToday.valueOf())
            };
          }))
          .filter((course: IBasicCourseWithSession) => course.sessions?.length > 0);
        this.past = routeData.courses?.map(((course: IBasicCourseWithSession) => {
            const startOfToday = new Date()
            startOfToday.setHours(0, 0, 0, 0);
            return {
              ...course,
              sessions: course.sessions.filter(session =>
                session.startDateTime < startOfToday.valueOf()
              )
            };
          }))
          .filter((course: IBasicCourseWithSession) => course.sessions?.length > 0)
          .sort((a: IBasicCourseWithSession, b: IBasicCourseWithSession) =>
            (b.lastStartDate ? new Date(b.lastStartDate).getTime() : (b.createdAt ? new Date(b.createdAt).getTime() : 0))
            - (a.lastStartDate ? new Date(a.lastStartDate).getTime() : (a.createdAt ? new Date(a.createdAt).getTime() : 0)));
        return this.route.queryParamMap;
      }),
      take(1),
      map((queryParamMap) => {
        if (queryParamMap.has("course_id")) {
          this.courseId = parseInt(<string>queryParamMap.get("course_id"));
          let sessionId = this.planified.find((course) => course.id === this.courseId)?.sessions[0].id;
          if (undefined === sessionId) {
            sessionId = this.past.find((course) => course.id === this.courseId)?.sessions[0].id;
          }
          if (sessionId) {
            this.selectSessionId = sessionId;
            this.userCourseApiService.getIsGpu(this.courseId).pipe(
              filter(isGpu => !isGpu),
              tap(() => this.launchSessionId = sessionId)
            ).subscribe();
          }
        }
        this.userRedirect = queryParamMap.get("user_redirect") ?? undefined;
      })
    ).subscribe();
    this.route.queryParamMap.subscribe(
      queryParamMap => {
        if(queryParamMap.has('session_id')) {
          this.selectSessionId = parseInt(<string>queryParamMap.get('session_id'));
        }

        if(queryParamMap.has('error_message')) {
          this.errorMessage = queryParamMap.get('error_message');
          const currentParams: { [key: string]: string | null } = { ...queryParamMap.keys.reduce((acc, key) => ({ ...acc, [key]: queryParamMap.get(key) }), {}) };
          delete currentParams['error_message'];

          this.router.navigate([], {
            queryParams: currentParams,
            replaceUrl: true
          });
        }
      }
    )
  }

  dismiss() {
    this.errorMessage = null;
  }

  ngAfterViewInit() {
    if (!this.selectSessionId) {
      return;
    }
    // mandatory to compute the right offset only when all accordions are initialized (and closed)
    setTimeout(() => {
      document.getElementById(this.getSessionHtmlId(
        this.selectSessionId
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

  getSessionHtmlId(sessionId: number | null): string {
    return `session-${sessionId}`;
  }

  getDaysUntil(date: number | string): string {
    if (null === date) {
      return ''
    }
    const startDate = new Date(date);
    const now = new Date();
    const diffTimeInDays = (startDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
    const absDiffTimeInDays = Math.abs(diffTimeInDays);
    const prefix = diffTimeInDays >= 0 ? "Dans": "Il y a"
    if (absDiffTimeInDays < 1) {
      return `${prefix} moins de 24 heures`
    }
    return `${prefix} ${Math.floor(absDiffTimeInDays)} ${absDiffTimeInDays < 2 ? 'jour': 'jours'}`;
  }
}
