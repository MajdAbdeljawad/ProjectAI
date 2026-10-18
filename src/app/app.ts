import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { NutritionService } from './core/services/nutrition.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  public nutritionService = inject(NutritionService);
  public profile = this.nutritionService.currentProfile;

  ngOnInit(): void {
    this.nutritionService.loadProfile().subscribe({
      error: () => {}
    });
  }

  getGoalLabel(goal?: string): string {
    const map: Record<string, string> = {
      perte_de_poids: 'Perte de poids',
      prise_de_masse: 'Prise de masse',
      maintien: 'Maintien',
      performance: 'Performance',
      reequilibrage: 'Rééquilibrage'
    };
    return (goal && map[goal]) ? map[goal] : (goal || 'Nutrition Sportive');
  }
}
