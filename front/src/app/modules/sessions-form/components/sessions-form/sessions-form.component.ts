import { ChangeDetectorRef, Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator, Validators
} from "@angular/forms";
import { IAdminSession, ISessionsById } from "../../interfaces/admin-session";
import { formatDate } from "@angular/common";
import { NgxPermissionsService } from "ngx-permissions";

@Component({
  selector: 'app-sessions-form',
  templateUrl: './sessions-form.component.html',
  styleUrls: ['./sessions-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SessionsFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SessionsFormComponent),
      multi: true,
    },
  ]
})
export class SessionsFormComponent implements OnInit, ControlValueAccessor, Validator {
  @Input() sessionsById?: ISessionsById;
  @Input() canEdit?: boolean;
  readonly sessionsFormArray: FormArray;

  private propagateChange = (_: IAdminSession[]) => {};
  private readonly dateFormat = 'dd/MM/YYYY \'à\' HH\'h\'mm';
  private readonly askDeploymentSubject = '[MyDocker] Programmation de déploiements';
  private readonly askSessionChangeSubject = '[MyDocker] Modification de session';
  private readonly askDeploymentBody = `Bonjour,
Merci de programmer les déploiements liés aux sessions de l'environnement ${window.location.href} .`
  private readonly askSessionChangeBody = `Bonjour,
Je souhaite modifier les sessions de l'environnement ${window.location.href} . Voici les changements souhaités : ...`
  private readonly emailAddress = 'support@example.com';
  readonly askDeploymentEmailLink = `mailto:${this.emailAddress}?subject=${encodeURIComponent(this.askDeploymentSubject)}&body=${encodeURIComponent(this.askDeploymentBody)}`;
  readonly askSessionChangeEmailLink = `mailto:${this.emailAddress}?subject=${encodeURIComponent(this.askSessionChangeSubject)}&body=${encodeURIComponent(this.askSessionChangeBody)}`;
  private isAdmin = false;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly cd: ChangeDetectorRef,
    private readonly permissionService: NgxPermissionsService,
  ) {
    this.sessionsFormArray = formBuilder.array([], Validators.required);
  }

  ngOnInit(): void {
    this.sessionsFormArray.valueChanges
      .subscribe(
        () => this.propagateChange(this.sessionsFormArray.getRawValue())
      );
    this.permissionService.hasPermission('ROLE_ADMIN').then((isAdmin) => {
      this.isAdmin = isAdmin;
      this.disableSessionControls();
    })
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.sessionsFormArray.invalid) {
      return { ports: this.sessionsFormArray.errors }
    }
    return null;
  }

  writeValue(obj: IAdminSession[]): void {
    // remove controls
    for (let i = this.sessionsFormArray.length - 1; i >= 0; i--) {
      this.sessionsFormArray.removeAt(i);
    }
    // set new value
    for (let i = 0; i < obj.length; i++) {
      this.sessionsFormArray.setControl(
        i,
        this.formBuilder.control(obj[i]),
      );
    }
    this.disableSessionControls();
  }

  private disableSessionControls(): void {
    this.sessionsFormArray.controls.forEach(control => {
      if (this.isSessionDisabled(control)) {
        control.disable();
      } else {
        control.enable();
      }
    });
  }

  addSession() {
    this.sessionsFormArray.push(
      this.formBuilder.control({})
    );
    this.cd.detectChanges();
    this.sessionsFormArray.updateValueAndValidity();
  }

  remove(index: number) {
    this.sessionsFormArray.removeAt(index);
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.sessionsFormArray.disable();
    } else {
      this.sessionsFormArray.enable();
    }
  }

  private getSession(control: AbstractControl): IAdminSession | undefined {
    if (!control.value?.id) {
      return undefined;
    }
    return control.value.id ? this.sessionsById?.[control.value.id] : undefined;
  }

  isSessionDisabled(control: AbstractControl): boolean {
    return this.isDeploymentPartiallyScheduled(control) && !this.isAdmin;
  }

  isDeploymentFullyScheduled(control: AbstractControl): boolean {
    const session = this.getSession(control);
    if (!session) {
      return false;
    }
    return session.cleanDeployment !== null && session.launchDeployment !== null;
  }

  isDeploymentPartiallyScheduled(control: AbstractControl): boolean {
    const session = this.getSession(control);
    if (!session) {
      return false;
    }
    return session.cleanDeployment !== null || session.launchDeployment !== null;
  }

  formatDeploymentTooltip(control: AbstractControl): string {
    const session = this.getSession(control);
    if (!session) {
      return '';
    }
    const tooltipParts: string[] = [];
    if (session.launchDeployment) {
      tooltipParts.push(`Lancement le ${formatDate(
        session.launchDeployment.startDateTime, this.dateFormat, 'fr',
      )}`);
    }
    if (session.cleanDeployment) {
      tooltipParts.push(`Repli le ${formatDate(
        session.cleanDeployment.startDateTime, this.dateFormat, 'fr',
      )}`);
    }
    return tooltipParts.join('\n');
  }
}
