import { ChangeDetectorRef, Component, forwardRef, Inject, Input, OnInit } from '@angular/core';
import {APP_CONFIG, IAppConfig} from "../../../../app-config";
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
import { TranslateService } from '@ngx-translate/core';

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
  readonly sessionsFormArray: FormArray;

  private propagateChange = (_: IAdminSession[]) => {};
  private readonly dateFormat = 'dd/MM/YYYY';
  private readonly timeFormat = 'HH\'h\'mm';
  private readonly emailAddress = 'support@example.com';
  private isAdmin = false;
  isDisabled = false;

  constructor(
    @Inject(APP_CONFIG) readonly config: IAppConfig,
    private readonly formBuilder: FormBuilder,
    private readonly cd: ChangeDetectorRef,
    private readonly permissionService: NgxPermissionsService,
    private readonly translate: TranslateService
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
      if (this.isSessionDisabled(control) || this.sessionsFormArray.disabled) {
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
      this.isDisabled = true;
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
      tooltipParts.push(
        this.translate.instant('admin.courses.edit.general_info.sessions.launch_on',
          {
            date: formatDate(session.launchDeployment.startDateTime, this.dateFormat, this.translate.currentLang),
            time: formatDate(session.launchDeployment.startDateTime, this.timeFormat, this.translate.currentLang),
          }
        ));
    }
    if (session.cleanDeployment) {
      tooltipParts.push(
        this.translate.instant('admin.courses.edit.general_info.sessions.clean_on',
          {
            date: formatDate(session.launchDeployment.startDateTime, this.dateFormat, this.translate.currentLang),
            time: formatDate(session.launchDeployment.startDateTime, this.timeFormat, this.translate.currentLang),
          }
        ));
    }
    return tooltipParts.join('\n');
  }

  get askDeploymentEmailLink() {
    const askDeploymentSubject = this.translate.instant('admin.courses.edit.general_info.sessions.ask_deployment_subject')
    const askDeploymentBody = this.translate.instant('admin.courses.edit.general_info.sessions.ask_deployment_body', { link: window.location.href })
    return `mailto:${this.emailAddress}?subject=${encodeURIComponent(askDeploymentSubject)}&body=${encodeURIComponent(askDeploymentBody)}`;
  }

  get askSessionChangeEmailLink() {
    const askSessionChangeSubject = this.translate.instant('admin.courses.edit.general_info.sessions.admin.courses.edit.general_info.sessions.ask_deployment_subject');
    const askSessionChangeBody = this.translate.instant('admin.courses.edit.general_info.sessions.admin.courses.edit.general_info.sessions.ask_deployment_body', { link: window.location.href });
    return `mailto:${this.emailAddress}?subject=${encodeURIComponent(askSessionChangeSubject)}&body=${encodeURIComponent(askSessionChangeBody)}`;
  }
}
