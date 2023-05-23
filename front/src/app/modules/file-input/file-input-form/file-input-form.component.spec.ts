import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FileInputFormComponent } from './file-input-form.component';

describe('FileInputFormComponent', () => {
  let component: FileInputFormComponent;
  let fixture: ComponentFixture<FileInputFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FileInputFormComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(FileInputFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
