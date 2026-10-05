import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FitnessApiService } from '../core/fitness-api.service';

@Component({
  selector: 'app-intake',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './intake.component.html',
  styleUrl: './intake.component.scss'
})
export class IntakeComponent {
  isGenerating = false;
  errorMessage = '';
  plan: any = null;

  readonly form: FormGroup;

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly fitnessApi: FitnessApiService
  ) {
    this.form = this.formBuilder.nonNullable.group({
      firstName: ['', Validators.required],
      age: [30, [Validators.required, Validators.min(16), Validators.max(100)]],
      gender: ['prefer not to say', Validators.required],
      heightCm: [175, [Validators.required, Validators.min(100), Validators.max(250)]],
      weightKg: [70, [Validators.required, Validators.min(30), Validators.max(300)]],
      fitnessLevel: ['beginner', Validators.required],
      trainingExperience: ['new to structured training', Validators.required],
      trainingDaysPerWeek: [3, [Validators.required, Validators.min(0), Validators.max(7)]],
      sessionDurationMinutes: [45, [Validators.required, Validators.min(15), Validators.max(240)]],
      trainingLocation: ['home', Validators.required],
      equipment: ['bodyweight only', Validators.required],
      mainGoal: ['muscle gain', Validators.required],
      targetWeightKg: [70, [Validators.min(30), Validators.max(300)]],
      injuriesOrLimitations: ['']
    });
  }

  generate(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isGenerating = true;
    this.errorMessage = '';
    this.plan = null;
    this.fitnessApi.generateGuestPlan(this.form.getRawValue()).subscribe({
      next: plan => {
        this.plan = plan;
        this.isGenerating = false;
      },
      error: error => {
        this.isGenerating = false;
        this.errorMessage = error?.error?.message || 'The coach could not generate a plan. Check that the backend is running.';
      }
    });
  }
}
