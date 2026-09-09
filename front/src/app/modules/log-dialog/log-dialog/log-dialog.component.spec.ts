import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { CommonModule } from "@angular/common";

import { LogDialogComponent } from './log-dialog.component';
import { IServiceLogs, ITaskLog } from "../../shell/interfaces/container";
import { TranslateTestingModule } from 'src/testing/translate-testing.module';

function taskLog(overrides: Partial<ITaskLog> = {}): ITaskLog {
  return {
    taskId: 'task-a',
    slot: 1,
    node: 'worker-01',
    createdAt: '2026-09-09T08:00:00Z',
    logs: 'first attempt',
    truncated: false,
    ...overrides,
  };
}

function render(data: IServiceLogs): ComponentFixture<LogDialogComponent> {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    declarations: [LogDialogComponent],
    imports: [CommonModule, MatDialogModule, TranslateTestingModule],
    providers: [
      { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: { close: () => undefined } },
    ],
  });
  const fixture = TestBed.createComponent(LogDialogComponent);
  fixture.detectChanges();
  return fixture;
}

describe('LogDialogComponent', () => {

  it('should create', () => {
    const fixture = render({ name: '42-7', image: 'ns/img', tasks: [] });
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('shows one block per task, in the order the API sent them', () => {
    const fixture = render({
      name: '42-7',
      image: 'ns/img',
      tasks: [
        taskLog({ taskId: 'first', logs: 'attempt one' }),
        taskLog({ taskId: 'second', slot: 2, node: 'worker-02', logs: 'attempt two' }),
      ],
    });

    const outputs = fixture.nativeElement.querySelectorAll('.log-dialog__output');
    expect(outputs.length).toBe(2);
    expect(outputs[0].textContent).toContain('attempt one');
    expect(outputs[1].textContent).toContain('attempt two');
  });

  it('hides the registry host in the image reference', () => {
    const fixture = render({
      name: '42-7',
      image: 'harbor.centralesupelec.fr/mydocker-vd-pp/test-mailhog-2',
      tasks: [],
    });

    expect(fixture.componentInstance.imageReference).toBe('mydocker-vd-pp/test-mailhog-2');
    expect(fixture.nativeElement.querySelector('code').textContent)
      .toContain('mydocker-vd-pp/test-mailhog-2');
  });

  it('says so when a task was truncated, and only for that task', () => {
    const fixture = render({
      name: '42-7',
      image: 'ns/img',
      tasks: [taskLog({ truncated: true }), taskLog({ taskId: 'other', truncated: false })],
    });

    expect(fixture.nativeElement.querySelectorAll('.log-dialog__truncated').length).toBe(1);
  });

  it('shows the empty message when there are no tasks', () => {
    const fixture = render({ name: '42-7', image: 'ns/img', tasks: [] });

    expect(fixture.nativeElement.querySelector('.log-dialog__empty')).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('.log-dialog__output').length).toBe(0);
  });

  it('renders a task with no node without failing', () => {
    const fixture = render({
      name: '42-7',
      image: 'ns/img',
      tasks: [taskLog({ node: '', createdAt: null })],
    });

    expect(fixture.nativeElement.querySelectorAll('.log-dialog__output').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.log-dialog__task-date')).toBeNull();
  });
});
