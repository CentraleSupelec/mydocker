import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { FileInputModule } from './file-input.module';
import { FileSelectDirective } from './file-select.directive';

@Component({
  selector: 'app-file-select-basic-test',
  template: `
    <input appFileSelect type="file" [multiple]="multiple" (fileSelect)="files = $event"/>
  `,
})
class FileSelectBasicTestComponent {
  multiple: boolean | undefined;
  disabled: boolean | undefined;
  files: FileList | File | undefined;
}

describe('Directive: FileSelect', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [FileSelectBasicTestComponent],
      imports: [FileInputModule],
    });
    TestBed.compileComponents();
  }));

  it('should add multiple attr', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileSelectBasicTestComponent);
      const component: FileSelectBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = true;
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileSelectDirective));
        expect((<any>directive.attributes).multiple).toBeDefined();
      });
    }),
  ));

  it('should throw (fileSelect) event for a single file', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileSelectBasicTestComponent);
      const component: FileSelectBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = false;
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileSelectDirective));
        directive.triggerEventHandler('change', {
          target: directive.nativeElement,
        });
      });
    }),
  ));
});

