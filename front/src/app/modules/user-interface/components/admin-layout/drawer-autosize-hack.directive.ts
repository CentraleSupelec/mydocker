import {Directive, ElementRef, Host, Inject, OnDestroy, OnInit, Optional, Self} from "@angular/core";
import {MatDrawer, MatDrawerContainer, MatSidenav, MatSidenavContainer} from "@angular/material/sidenav";
import {DOCUMENT} from "@angular/common";
import {Subject} from "rxjs";
import {takeUntil} from "rxjs/operators";

// From https://github.com/angular/components/issues/9837#issuecomment-998439921
@Directive({
  selector: "[appDrawerAutosizeHack]",
})
export class DrawerAutosizeHackDirective implements OnInit, OnDestroy {
  constructor(
    @Optional() @Self() @Host() drawer: MatDrawer,
    @Optional() @Self() @Host() sidenav: MatSidenav,
    @Optional() drawerContainer: MatDrawerContainer,
    @Optional() sidenavContainer: MatSidenavContainer,
    @Optional() @Inject(DOCUMENT) private _doc: Document,
    private _el: ElementRef<HTMLElement>
  ) {
    this._drawer = drawer ?? sidenav;
    this._container = drawerContainer ?? sidenavContainer;
  }

  private _drawer?: MatDrawer | MatSidenav;
  private _container?: MatDrawerContainer | MatSidenavContainer;

  private _destroyed = new Subject<void>();

  private resizeObserver?: ResizeObserver;

  ngOnInit(): void {
    this._drawer?.openedChange
      .pipe(takeUntil(this._destroyed))
      .subscribe(() => {
        this._doc
          .querySelector(".mat-drawer-transition")
          ?.classList.remove("mat-drawer-transition");
      });

    this.resizeObserver = new ResizeObserver((entries) => {
      this._container?.updateContentMargins();
    });
    this.resizeObserver.observe(this._el.nativeElement);
  }

  ngOnDestroy(): void {
    this._destroyed.next();
    this._destroyed.complete();

    this.resizeObserver?.unobserve(this._el.nativeElement);
  }
}
