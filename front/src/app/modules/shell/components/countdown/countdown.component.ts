import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { timer, Subscription } from 'rxjs';
import { ContainerApiService } from '../../services/container-api.service';
import { SnackNotificationService } from '../../../utils/snack-notification/snack-notification.service';

@Component({
  selector: 'app-countdown',
  templateUrl: './countdown.component.html',
  styleUrls: ['./countdown.component.scss']
})
export class CountdownComponent implements OnInit, OnDestroy {

  @Input() targetDatetime?: number;
  @Input() sessionId?: number;
  @Output() timerElapsed = new EventEmitter<void>();

  private subscription?: Subscription;
  private milliSecondsInASecond = 1000;
  private hoursInADay = 24;
  private minutesInAnHour = 60;
  private SecondsInAMinute = 60;
  private timeDifference?: number;

  secondsToDday?: number;
  minutesToDday?: number;
  hoursToDday?: number;
  delayPending = false;

  constructor(
    private readonly containerApiService: ContainerApiService,
    private readonly snackNotificationService: SnackNotificationService,
  ) {
  }

  private getTimeDifference() {
    this.timeDifference = this.milliSecondsInASecond * (this.targetDatetime as number) - new Date().getTime();
    this.allocateTimeUnits(this.timeDifference);
    if (this.timeDifference < 0) {
      this.subscription?.unsubscribe();
      this.timerElapsed.emit();
    }
  }

  private allocateTimeUnits(timeDifference: any) {
    this.secondsToDday = Math.max(0, Math.floor((timeDifference) / (this.milliSecondsInASecond) % this.SecondsInAMinute));
    this.minutesToDday = Math.max(0, Math.floor((timeDifference) / (this.milliSecondsInASecond * this.minutesInAnHour) % this.SecondsInAMinute));
    this.hoursToDday = Math.max(0, Math.floor((timeDifference) / (this.milliSecondsInASecond * this.minutesInAnHour * this.SecondsInAMinute) % this.hoursInADay));
  }

  ngOnInit() {
    this.delayDeletion();
    this.subscription = timer(0, 1000)
      .subscribe(() => {
        this.getTimeDifference();
      });
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe();
  }

  delayDeletion() {
    this.delayPending = true;
    this
      .containerApiService
      .delayDeletion(this.sessionId)
      .subscribe((newDeletionTime) => {
        this.delayPending = false;
        this.targetDatetime = newDeletionTime.deletionTime;
        this.snackNotificationService.push(`L'extinction a bien été retardée`, 'success');
      }, () => {
        this.delayPending = false;
        this.snackNotificationService.push(`Impossible de retarder l'extinction de l'environnement`, 'error');
      });
  }

}
