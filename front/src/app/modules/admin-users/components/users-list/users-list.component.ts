import { Component, OnInit, ViewChild } from '@angular/core';
import { merge, Subject } from "rxjs";
import { MatPaginator } from "@angular/material/paginator";
import { mergeMap } from "rxjs/operators";
import { IUser } from "../../../permissions/interfaces/user";
import { UserApiService } from "../../service/user-api.service";
import { MatSort } from "@angular/material/sort";
import { debounceTime, tap } from "rxjs/operators";
import { Roles } from "../../interfaces/roles";

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.css']
})
export class UsersListComponent implements OnInit {
  users: IUser[] = [];
  usersSize: number | undefined;
  query: string = '';
  roles: string[] = ['ROLE_TEACHER', 'ROLE_ADMIN'];
  readonly availableRoles = Object.entries(Roles)
  displayedColumns = ['role', 'email', 'name', 'lastname', 'action'];

  @ViewChild(MatPaginator, {static: true}) paginator: MatPaginator | undefined;
  @ViewChild(MatSort, {static: true}) sort: MatSort | undefined;

  readonly searchChange$: Subject<void> = new Subject<void>();

  constructor(
    private readonly userApiService: UserApiService
  ) { }

  ngOnInit(): void {
    if (this.paginator && this.sort) {
      merge(
        this.paginator.page,
        this.sort.sortChange.pipe(
          tap(() => {
            if (this.paginator) {
              this.paginator.pageIndex = 0;
            }
          })
        ),
        this.searchChange$
          .pipe(
            debounceTime(150),
            tap(() => {
              if (this.paginator) {
                this.paginator.pageIndex = 0;
              }
            }),
          ),
      ).pipe(
        mergeMap(() => {
          return this.userApiService.getUsers(
            this.query,
            this.roles,
            this.paginator?.pageIndex,
            this.paginator?.pageSize,
            this.sort?.active,
            this.sort?.direction || 'asc'
          )
        })
      ).subscribe(
        page => {
          this.users = page.content
          this.usersSize = page.totalElements
        }
      )
      this.paginator.pageSize = 25
      this.paginator.pageIndex = 0
      this.sort.active = 'lastname'
      this.sort.direction = 'asc'
    }
    this.searchChange$.next();
  }


  clearQuery() {
    this.query = '';
    this.searchChange$.next()
  }

  updateQuery() {
    this.searchChange$.next()
  }
}
