import { Component, ElementRef, Input, OnChanges, OnDestroy, OnInit, Renderer2, SimpleChanges, ViewChild } from '@angular/core';
import { IHttpPortDisplay, IPortDisplay } from "../../admin-course/interfaces/course-display";
import { IContainerPort } from "../../shell/interfaces/container-port";
import { RenderStringService } from "../render-string.service";
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-display-custom-container-port',
  templateUrl: './display-custom-container-port.component.html',
  styleUrls: ['./display-custom-container-port.component.css']
})
export class DisplayCustomContainerPortComponent implements OnInit, OnDestroy, OnChanges {
  @ViewChild('contentContainer') container!: ElementRef<HTMLDivElement>;

  @Input() customDisplay: IPortDisplay | null = null;
  @Input() containerPorts: IContainerPort[] | undefined = [];
  @Input() username: string | undefined = '';
  @Input() ipAddress: string | undefined = '';
  @Input() password: string | undefined = '';
  @Input() userRedirect: string | undefined = '';
  @Input() autoClick: boolean = false;
  private sub!: Subscription | undefined;

  constructor(
    private readonly renderStringService: RenderStringService,
    private renderer: Renderer2
  ) {
  }

  ngOnInit(): void {
    if (this.autoClick) {
      window.open(this.url(), '_blank');
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  ngOnChanges(changes: SimpleChanges): void {
      if (changes.customDisplay) {
        setTimeout(() => this.addCopyButtons());
      }
  }
  title() {
    return (this.customDisplay as IHttpPortDisplay)?.title;
  }

  url() {
    return this.renderStringService.renderString(
      (this.customDisplay as IHttpPortDisplay)?.url,
      this.containerPorts ? this.containerPorts : [],
      this.username ? this.username : '',
      this.password ? this.password : '',
      this.ipAddress ? this.ipAddress : '',
      this.userRedirect ? this.userRedirect : ''
    )
  }

  isCustomDisplayType(customDisplayType: string) {
    return (this.customDisplay as IHttpPortDisplay)?.customDisplayType === customDisplayType;
  }

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
