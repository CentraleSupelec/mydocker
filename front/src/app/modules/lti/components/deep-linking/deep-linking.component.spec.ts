import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeepLinkingComponent } from './deep-linking.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { APP_CONFIG } from '../../../../app-config';

describe('DeepLinkingComponent', () => {
  let component: DeepLinkingComponent;
  let fixture: ComponentFixture<DeepLinkingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DeepLinkingComponent ],
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
      ],
      providers: [
        {
          provide: APP_CONFIG,
          useValue: {},
        },
      ],
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DeepLinkingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
