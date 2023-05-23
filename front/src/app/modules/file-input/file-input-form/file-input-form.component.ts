import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  Output,
  Renderer2,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Observable, Subject } from 'rxjs';

@Component({
  selector: 'app-file-input-form',
  templateUrl: './file-input-form.component.html',
  styleUrls: ['./file-input-form.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FileInputFormComponent),
      multi: true,
    },
  ],
})
export class FileInputFormComponent implements ControlValueAccessor {
  private _multiple = false;
  private _disabled = false;
  private _value: any;
  private _subjectValueChanges: Subject<any>;
  valueChanges: Observable<any>;

  @ViewChild('fileInput', { static: true }) _inputElement: ElementRef | undefined;
  get inputElement(): HTMLInputElement {
    return this._inputElement?.nativeElement;
  }
  @Input() color: 'accent' | 'primary' | 'warn' = 'primary';

  @Input()
  set multiple(multiple: boolean) {
    this._multiple = coerceBooleanProperty(multiple);
  }
  get multiple(): boolean {
    return this._multiple;
  }

  @Input() accept: string = '';

  @Output() selectFiles: EventEmitter<File | FileList> = new EventEmitter<File | FileList>();

  constructor(private _renderer: Renderer2, private _changeDetectorRef: ChangeDetectorRef) {
    this._subjectValueChanges = new Subject<any>();
    this.valueChanges = this._subjectValueChanges.asObservable();
  }

  /**
   * Method executed when a file is selected.
   */
  handleSelect(files: File | FileList): void {
    this.writeValue(files);
    this.selectFiles.emit(files);
  }

  /**
   * Used to clear the selected files from the [FileInputComponent].
   */
  clear(): void {
    this.writeValue(null);
    this._renderer.setProperty(this.inputElement, 'value', '');
  }

  /** Method executed when the disabled value changes */
  onDisabledChange(v: boolean): void {
    if (v) {
      this.clear();
    }
  }
  /**
   * Sets disable to the component. Implemented as part of ControlValueAccessor.
   */
  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  get disabled(): boolean {
    return this._disabled;
  }

  @Input()
  set disabled(value: boolean) {
    const newValue: boolean = coerceBooleanProperty(value);
    if (this._disabled !== newValue) {
      this._disabled = newValue;
      this.onDisabledChange(this._disabled);
    }
  }


  set value(v: any) {
    if (v !== this._value) {
      this._value = v;
      this.onChange(v);
      this._changeDetectorRef.markForCheck();
      this._subjectValueChanges.next(v);
    }
  }
  get value(): any {
    return this._value;
  }

  writeValue(value: any): void {
    this.value = value;
    this._changeDetectorRef.markForCheck();
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  onChange = (_: any) => () => {};
}
