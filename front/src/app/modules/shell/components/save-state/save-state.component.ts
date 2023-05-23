import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject, timer } from "rxjs";
import { ISaveState } from "../../interfaces/save-state";
import { mergeMap, takeUntil } from "rxjs/operators";
import { IBasicCourse } from "../../interfaces/course";
import { ContainerApiService } from "../../services/container-api.service";
import { ObservableSnackNotificationService } from "../../../utils/snack-notification/observable-snack-notification.service";

@Component({
  selector: 'app-save-state',
  templateUrl: './save-state.component.html',
  styleUrls: ['./save-state.component.css']
})
export class SaveStateComponent implements OnInit, OnDestroy {
  @Input() course: IBasicCourse | undefined;

  saveState: ISaveState | null = null;

  private readonly stopSavePolling$: Subject<void> = new Subject<void>();
  private readonly refreshSaveStatus$: Subject<void> = new Subject<void>();

  constructor(
    private readonly containerApiService: ContainerApiService,
    private readonly toastService: ObservableSnackNotificationService,
  ) { }

  ngOnInit() {
    this.refreshSaveStatus$
      .pipe(
        mergeMap(() => timer(0,5000)),
        mergeMap(() => this.containerApiService.getSaveState(this.course?.id)),
        takeUntil(this.stopSavePolling$),
      ).subscribe(
      saveState => {
        if (this.saveState?.lastSaveError !== saveState.lastSaveError || this.saveState.savedAt !== saveState.savedAt ) {
          this.saveState = saveState;
          this.stopSavePolling$.next();
        }
      }
    )

    this.refreshSaveStatus$.next();
  }

  ngOnDestroy(): void {
    this.stopSavePolling$.next();
  }

  saveData() {
    this.toastService.toast(
      this.containerApiService.saveState(this.course?.id),
      'Début de la sauvegarde de votre travail',
      'Nous avons recontré une erreur lors de la sauvegarde ...'
    )
  }

}
