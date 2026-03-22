import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Farmacia } from './farmacia';

describe('Farmacia', () => {
  let component: Farmacia;
  let fixture: ComponentFixture<Farmacia>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Farmacia]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Farmacia);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
