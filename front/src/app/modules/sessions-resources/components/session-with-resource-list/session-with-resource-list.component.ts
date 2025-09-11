import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ISessionWithResources } from "../../interfaces/session-with-resources";
import { SessionWithResourcesApiService } from "../../services/session-with-resources-api.service";
import { MatTableDataSource } from "@angular/material/table";
import { MatSort } from "@angular/material/sort";
import { FormBuilder, FormControl } from "@angular/forms";
import { startWith, switchMap } from "rxjs/operators";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";

interface ISessionWithResourcesList extends ISessionWithResources {
  hasResource: boolean;
}

@Component({
  selector: 'app-session-with-resource-list',
  templateUrl: './session-with-resource-list.component.html',
  styleUrls: ['./session-with-resource-list.component.css']
})
export class SessionWithResourceListComponent implements OnInit, OnDestroy {
  sessions: MatTableDataSource<ISessionWithResourcesList> = new MatTableDataSource<ISessionWithResourcesList>();
  displayedColumns = ['hasResource', 'title', 'startDateTime', 'endDateTime', 'action'];
  readonly formControl: FormControl;
  private readonly destroy$: Subject<void> = new Subject();

  @ViewChild(MatSort, {static: true}) sort: MatSort | null = null;

  constructor(
    formBuilder: FormBuilder,
    private readonly sessionWithResourcesApiService: SessionWithResourcesApiService,
  ) {
    this.formControl = formBuilder.control(
      new Date().getTime()
    )
  }

  ngOnInit(): void {
    this.sessions.sort = this.sort;
    this.formControl.valueChanges.pipe(
      startWith(new Date().getTime()),
      switchMap(v => this.sessionWithResourcesApiService.getSessions(v)),
      takeUntil(this.destroy$)
    )
      .subscribe(
        sessions => this.sessions.data = sessions.map(
          session => {
            return {
              ...session,
              hasResource: session.resources.length !== 0
            };
          }
        )
      );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
  }

}
