import { AfterViewInit, ViewChild, Renderer2, Component, forwardRef, Input, OnDestroy, OnInit, ElementRef } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from "@angular/forms";
import { RenderStringService } from "../../../display-container/render-string.service";
import { IContainerPort } from "../../../shell/interfaces/container-port";
import { Subject, Subscription } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { CustomDisplayType, IHttpPortDisplay } from "../../interfaces/course-display";

@Component({
  selector: 'app-course-display-url',
  templateUrl: './course-display-url.component.html',
  styleUrls: ['./course-display-url.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CourseDisplayUrlComponent),
      multi: true
    }
  ]
})
export class CourseDisplayUrlComponent implements OnInit, ControlValueAccessor, OnDestroy {
  @ViewChild('contentContainer') container!: ElementRef<HTMLDivElement>;
  @Input() ports: IContainerPort[] = [];
  private sub!: Subscription | undefined;

  readonly formGroup: FormGroup;
  private readonly destroy$ = new Subject<void>();
  readonly customDisplayType = Object.entries(CustomDisplayType);

  constructor(
    formBuilder: FormBuilder,
    private readonly renderStringService: RenderStringService,
    private renderer: Renderer2
  ) {
    this.formGroup = formBuilder.group({
      title: '',
      url: '',
      customDisplayType: ''
    });
  }

  ngOnInit(): void {
    this.formGroup.valueChanges
      .pipe(
        takeUntil(this.destroy$)
      )
      .subscribe(
        v => this.propagateChange({
          ...v,
          type: 'HTTP'
        })
      )
    this.sub = this.formGroup.get('url')?.valueChanges.subscribe(() => {
        setTimeout(() => this.addCopyButtons());
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.sub?.unsubscribe();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  writeValue(obj: IHttpPortDisplay): void {
    this.formGroup.setValue({
      title: obj?.title || '',
      url: obj?.url || '',
      customDisplayType: obj?.customDisplayType || ''
    })
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formGroup.disable();
    } else {
      this.formGroup.enable();
    }
  }

  renderString(url: string): string {
    return this.renderStringService.renderString(
      url, this.ports, 'USERNAME', 'PASSWORD', 'IP_ADDRESS', 'USER_REDIRECT'
    )
  }

  private propagateChange(_ : any) {}

  addCopyButtons() {
    if (this.container === undefined) {
      return;
    }

    const codes = this.container.nativeElement.querySelectorAll('code');

    codes.forEach(code => {
      if (code.parentElement?.querySelector('.mat-icon-button')) return;

      if (code.parentNode !== null) {
        this.replacePWithDiv(code.parentNode)
        this.renderer.setStyle(code.parentNode, 'display', 'flex')
      }

      const wrapper = this.renderer.createElement('div');
      this.renderer.setStyle(wrapper, 'display', 'flex');
      this.renderer.setStyle(wrapper, 'align-items', 'center');
      this.renderer.setStyle(wrapper, 'gap', '4px');

      this.renderer.insertBefore(code.parentNode, wrapper, code);
      this.renderer.appendChild(wrapper, code);


      const matDiv = this.renderer.createElement('div');
      this.renderer.addClass(matDiv, 'mat-form-field-suffix');
      this.renderer.addClass(matDiv, 'ng-star-inserted');

      matDiv.innerHTML = `
        <button mat-icon-button="" matsuffix="" class="mat-focus-indicator mat-icon-button mat-button-base" style="display: flex; align-items: center;">
          <span class="mat-button-wrapper">
            <mat-icon role="img" class="mat-icon notranslate material-icons mat-ligature-font mat-icon-no-color" aria-hidden="true" data-mat-icon-type="font">
              content_copy
            </mat-icon>
          </span>
          <span matripple="" class="mat-ripple mat-button-ripple mat-button-ripple-round"></span>
          <span class="mat-button-focus-overlay"></span>
        </button>
      `;

      this.renderer.appendChild(wrapper, matDiv);

      const button = matDiv.querySelector('button');
      if (button) {
        this.renderer.listen(button, 'click', () => {
          navigator.clipboard.writeText(code.textContent || '');
        });
      }

    });
  }

  replacePWithDiv(p: ParentNode) {
    const div = this.renderer.createElement('div');

    while (p.firstChild) {
      this.renderer.appendChild(div, p.firstChild);
    }

    const parent = p.parentNode;
    if (parent) {
      this.renderer.insertBefore(parent, div, p);
      this.renderer.removeChild(parent, p);
    }

    return div;
  }
}
