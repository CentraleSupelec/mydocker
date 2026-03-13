import { Component, Inject, Input, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { animate, state, style, transition, trigger } from "@angular/animations";
import { CourseStatus, IAdminCourse } from "../../interfaces/course";
import { AdminCoursesApiService } from "../../services/admin-courses-api.service";
import { APP_CONFIG, IAppConfig } from "../../../../app-config";
import { merge, Subject } from "rxjs";
import { debounceTime, mergeMap, tap } from "rxjs/operators";
import { MatSort } from "@angular/material/sort";
import { MatPaginator } from "@angular/material/paginator";

@Component({
  selector: 'app-courses-list',
  templateUrl: './courses-list.component.html',
  styleUrls: ['./courses-list.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({minHeight: '0', height: '0', overflow: 'hidden'})),
      state('expanded', style({height: '*', paddingTop: '15px'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ],
})
export class CoursesListComponent implements OnInit, OnDestroy {
  columnsToDisplay = ['icon', 'title', 'creator', 'createdOn', 'numberOfUsers', 'numberOfConnectedUsers', 'numberOfRecentUsers', 'action'];
  courses: IAdminCourse[] = [];
  courseSize: number | undefined;
  formGroup!: FormGroup;
  readonly availableStatus = Object.entries(CourseStatus)

  @ViewChild(MatPaginator, {static: true}) paginator!: MatPaginator;
  @ViewChild(MatSort, {static: true}) sort!: MatSort;

  readonly searchChange$: Subject<void> = new Subject<void>();

  @Input() stopContainerPolling$: Subject<void> = new Subject<void>();
  @Input() buttonCell: TemplateRef<any> | null = null;
  @Input() expandedRow: TemplateRef<any> | null = null;
  @Input() expandedElement: IAdminCourse | null = null;

  constructor(
    private readonly adminCourseApiService: AdminCoursesApiService,
    private readonly fb: FormBuilder,
    @Inject(APP_CONFIG) readonly config: IAppConfig,
  ) {}

  ngOnInit(): void {
    const threeMonthsAgo = new Date();
    threeMonthsAgo.setMonth(threeMonthsAgo.getMonth() - 3);
    this.formGroup = this.fb.group({
      query: [''],
      status: [['DRAFT','TEST','READY']],
      dateLimit: [threeMonthsAgo.valueOf()]
    });

    if (this.paginator && this.sort) {

      this.formGroup.valueChanges
        .pipe(debounceTime(150))
        .subscribe(() => {
          this.paginator.pageIndex = 0;
          this.searchChange$.next();
        });

      merge(
        this.paginator.page,
        this.sort.sortChange.pipe(
          tap(() => this.paginator.pageIndex = 0)
        ),
        this.searchChange$
      )
      .pipe(
        mergeMap(() => {
          const { query, status, dateLimit } = this.formGroup.value;

          return this.adminCourseApiService.getCourses(
            query,
            status,
            dateLimit,
            this.paginator.pageIndex,
            this.paginator.pageSize,
            this.sort.active,
            this.sort.direction || 'asc'
          )
        })
      )
      .subscribe(page => {
        this.courses = page.content
        this.courseSize = page.totalElements
      });

      this.paginator.pageSize = 25;
      this.paginator.pageIndex = 0;

      this.sort.active = 'createdOn';
      this.sort.direction = 'desc';
    }

    this.searchChange$.next();
  }

  ngOnDestroy(): void {
    this.stopContainerPolling$.next();
  }

  clearQuery() {
    this.formGroup.patchValue({ query: '' });
  }
}
