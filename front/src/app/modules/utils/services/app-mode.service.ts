import {Injectable} from "@angular/core";
import {BehaviorSubject, Observable} from "rxjs";

export enum APP_MODE {
  REGULAR = 'REGULAR',
  LTI = 'LTI'
}
@Injectable({
  providedIn: 'root'
})
export class AppModeService {
  private readonly _mode = new BehaviorSubject<APP_MODE>(APP_MODE.REGULAR);
  get mode(): Observable<APP_MODE> {
    return this._mode.asObservable();
  }
  public setMode(newMode: APP_MODE) {
    this._mode.next(newMode);
  }
}
