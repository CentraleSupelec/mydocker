import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { FileInputModule } from './file-input.module';
import { FileDropDirective } from './file-drop.directive';

@Component({
  selector: 'app-file-drop-basic-test',
  template: `
    <div appFileDrop [multiple]="multiple" [disabled]="disabled" (fileDrop)="files = $event"></div>
  `,
})
class FileDropBasicTestComponent {
  multiple: boolean | undefined;
  disabled: boolean | undefined;
  files: FileList | File | undefined;
}

describe('Directive: FileDrop', () => {
  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [FileDropBasicTestComponent],
      imports: [FileInputModule],
    });
    TestBed.compileComponents();
  }));

  it('should add/remove class on dragenter and dragleave', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = false;
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        directive.triggerEventHandler('dragenter', new Event('dragenter'));
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect(directive.classes['drop-zone']).toBeTruthy();
          directive.triggerEventHandler('dragleave', new Event('dragleave'));
          fixture.detectChanges();
          fixture.whenStable().then(() => {
            expect(directive.classes['drop-zone']).toBeFalsy();
          });
        });
      });
    }),
  ));

  it('should disable element and not add class on dragenter', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.disabled = true;
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        directive.triggerEventHandler('dragenter', new Event('dragenter'));
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect(directive.classes['drop-zone']).toBeFalsy();
        });
      });
    }),
  ));

  it('should throw dragover event and add copy dropEffect for a single file', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = false;
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        const event: any = <DragEvent>new Event('dragover');
        event.dataTransfer = {
          dropEffect: 'none',
          types: ['Files'],
          items: ['file-name.txt'],
        };
        directive.triggerEventHandler('dragover', event);
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect(event.dataTransfer.dropEffect).toBe('copy');
        });
      });
    }),
  ));

  it('should throw dragover event and not add copy dropEffect for a multiple file', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = false;
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        const event: any = <DragEvent>new Event('dragover');
        event.dataTransfer = {
          dropEffect: 'none',
          types: ['Files'],
          items: ['file-name.txt', 'file-name1.txt'],
        };
        directive.triggerEventHandler('dragover', event);
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect(event.dataTransfer.dropEffect).toBe('none');
        });
      });
    }),
  ));

  it('should throw dragover event and add copy dropEffect for a multiple file', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = true;
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        const event: any = <DragEvent>new Event('dragover');
        event.dataTransfer = {
          dropEffect: 'none',
          types: ['Files'],
          items: ['file-name.txt', 'file-name1.txt'],
        };
        directive.triggerEventHandler('dragover', event);
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect(event.dataTransfer.dropEffect).toBe('copy');
        });
      });
    }),
  ));

  it('should throw dragover event and not add copy dropEffect on disabled state', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = false;
      component.disabled = true;
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        const event: any = <DragEvent>new Event('dragover');
        event.dataTransfer = {
          dropEffect: 'none',
          types: ['Files'],
          items: ['file-name.txt'],
        };
        directive.triggerEventHandler('dragover', event);
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect(event.dataTransfer.dropEffect).toBe('none');
        });
      });
    }),
  ));

  it('should throw ondrop event for a single file', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = false;
      expect(component.files).toBeFalsy();
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        const event: any = <DragEvent>new Event('drop');
        event.dataTransfer = {
          files: [{}],
        };
        directive.triggerEventHandler('drop', event);
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect(component.files).toBeTruthy();
        });
      });
    }),
  ));

  it('should throw ondrop event for a multiple files', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.multiple = true;
      expect(component.files).toBeFalsy();
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        const event: any = <DragEvent>new Event('drop');
        event.dataTransfer = {
          files: [{}, {}],
        };
        directive.triggerEventHandler('drop', event);
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect((<FileList>component.files).length).toBe(2);
        });
      });
    }),
  ));

  it('should not throw ondrop event for disabled state', waitForAsync(
    inject([], () => {
      const fixture: ComponentFixture<any> = TestBed.createComponent(FileDropBasicTestComponent);
      const component: FileDropBasicTestComponent = fixture.debugElement.componentInstance;
      component.disabled = true;
      expect(component.files).toBeFalsy();
      fixture.detectChanges();
      fixture.whenStable().then(() => {
        const directive: DebugElement = fixture.debugElement.query(By.directive(FileDropDirective));
        const event: any = <DragEvent>new Event('drop');
        event.dataTransfer = {
          files: [{}],
        };
        directive.triggerEventHandler('drop', event);
        fixture.detectChanges();
        fixture.whenStable().then(() => {
          expect(component.files).toBeFalsy();
        });
      });
    }),
  ));
});

