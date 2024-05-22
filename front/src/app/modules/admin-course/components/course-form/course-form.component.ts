import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from "@angular/forms";
import {
  CourseStatus,
  IAdminCourse,
  IAdminUpdateCourse,
} from "../../interfaces/course";
import { IComputeType } from '../../../compute-type/interfaces/compute-type';
import { ISessionsById } from "../../../sessions-form/interfaces/admin-session";

const DEFAULT_COMMAND = "${MYDOCKER_USERNAME} ${MYDOCKER_PASSWORD}"

@Component({
  selector: 'app-course-form',
  templateUrl: './course-form.component.html',
  styleUrls: ['./course-form.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CourseFormComponent),
      multi: true,
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CourseFormComponent),
      multi: true,
    },
  ]
})
export class CourseFormComponent implements OnInit, ControlValueAccessor, Validator {
  readonly courseForm: FormGroup;
  readonly courseStatus = Object.entries(CourseStatus);
  defaultComputeTypeId?: number;
  @Input() computeTypes?: Array<IComputeType>;
  @Input() sessionsById?: ISessionsById;

  constructor(
    formBuilder: FormBuilder,
  ) {
    this.courseForm = formBuilder.group({
      general: {},
      technical: {}
    });
  }

  ngOnInit(): void {
    this.defaultComputeTypeId = this.computeTypes?.find((computeType) => computeType.technicalName === '')?.id;
    this.courseForm.valueChanges
      .subscribe(
        v => {
          this.propagateChange({...v.general, ...v.technical});
        }
      )
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.courseForm.disable();
    } else {
      this.courseForm.enable();
    }
  }

  validate(control: AbstractControl): ValidationErrors | null {
    if (this.courseForm.invalid) {
      return {courseErrors: this.courseForm.errors}
    }
    return null;
  }

  writeValue(obj: IAdminCourse): void {
    this.courseForm.setValue({
      general: {
        title: obj?.title || '',
        description: obj?.description || '',
        status: obj?.status || CourseStatus.DRAFT,
        sessions: obj?.sessions || [],
        shutdownAfterMinutes: obj?.shutdownAfterMinutes,
        warnShutdownMinutes: obj?.warnShutdownMinutes,
      },
      technical: {
        ports: obj?.ports || [],

        dockerImage: obj?.dockerImage || '',
        nanoCpusLimit: obj?.nanoCpusLimit || null,
        memoryBytesLimit: obj?.memoryBytesLimit || null,
        computeTypeId: obj?.computeTypeId || this.defaultComputeTypeId,
        command: undefined === obj?.command ? DEFAULT_COMMAND : obj?.command,

        saveStudentWork: obj?.saveStudentWork || false,
        workdirSize: obj?.workdirSize || null,
        workdirPath: obj?.workdirPath || null,
        allowStudentToSubmit: obj?.allowStudentToSubmit || false,

        displayOptions: obj?.displayOptions || {},
        useStudentVolume: obj?.useStudentVolume || false,
        studentVolumePath: obj?.studentVolumePath || null,
      }
    });
  }

  private propagateChange = (_: IAdminUpdateCourse) => {}
}
