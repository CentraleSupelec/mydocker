import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BasicLayoutComponent } from "./components/basic-layout/basic-layout.component";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { RouterModule } from "@angular/router";
import { FlexLayoutModule } from "@angular/flex-layout";
import { NgxPermissionsModule } from "ngx-permissions";
import { MatMenuModule } from "@angular/material/menu";
import { MatTooltipModule } from "@angular/material/tooltip";
import { AdminLayoutComponent } from './components/admin-layout/admin-layout.component';
import { MatSidenavModule } from "@angular/material/sidenav";
import { MatListModule } from "@angular/material/list";
import { BreadcrumbModule } from "../utils/breadcrumb/breadcrumb.module";
import {DrawerAutosizeHackDirective} from "./components/admin-layout/drawer-autosize-hack.directive";


@NgModule({
  declarations: [
    BasicLayoutComponent,
    AdminLayoutComponent,
    DrawerAutosizeHackDirective,
  ],
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    RouterModule,
    FlexLayoutModule,
    NgxPermissionsModule,
    MatMenuModule,
    MatTooltipModule,
    MatSidenavModule,
    MatListModule,
    BreadcrumbModule,
  ]
})
export class UserInterfaceModule { }
