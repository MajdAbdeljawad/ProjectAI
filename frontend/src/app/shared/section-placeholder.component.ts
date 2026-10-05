import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-section-placeholder',
  standalone: true,
  template: `
    <main class="placeholder">
      <p class="eyebrow">FitCoach workspace</p>
      <h1>{{ title }}</h1>
      <p>This section is ready for the next product module.</p>
    </main>
  `,
  styles: [`
    :host { display: block; }
    .placeholder { min-height: 50vh; display: grid; align-content: center; gap: 0.75rem; padding: 2rem; max-width: 720px; margin: auto; }
    .eyebrow { color: #34d399; font-size: 0.8rem; letter-spacing: 0.08em; text-transform: uppercase; }
    h1 { margin: 0; font-size: clamp(2rem, 5vw, 4rem); }
    p { color: #91a4bd; }
  `]
})
export class SectionPlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  readonly title = this.route.snapshot.data['title'] as string;
}
