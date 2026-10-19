# Carte de contribution — module Progress (ami)

> Document à présenter séparément au professeur : contribution individuelle sur FitCoach.

## Qui / quoi

| | |
|---|---|
| **Module** | Suivi de progression + coach IA |
| **Page** | `/progress` |
| **API** | `POST /api/public/progress/analyze` |
| **Doc technique + prompts** | [PROMPTS_PROGRESS.md](PROMPTS_PROGRESS.md) |
| **Captures** | `docs/screenshots/05` à `08` |

*(Remplace cette ligne par le nom de ton ami avant de rendre le devoir.)*

## En une phrase

Transformer le placeholder **Progress** en suivi de poids avec calculs backend (tendance, date estimée) et commentaire IA (Ollama), sans compte utilisateur.

## Livrables visibles

1. Formulaire objectif + mesures (stockage navigateur)
2. Cartes stats : poids actuel, variation, tendance/semaine, date estimée
3. Graphique SVG (courbe + ligne du poids cible)
4. Commentaire coach + 3 conseils
5. Bouton « Show the prompt sent to the AI »
6. Tests JUnit 5 + Mockito
7. Fichier `PROMPTS_PROGRESS.md` (prompts + techniques + captures)

## Stack utilisée

Angular (standalone) · Spring Boot · Jakarta Validation · Ollama · H2/MySQL

## Preuve pour le prof

| Capture | Contenu |
|---|---|
| `05-progress-form.png` | Formulaire et mesures |
| `06-progress-ai-result.png` | Stats + graphique |
| `07-progress-prompt.png` | Prompt exact envoyé à l’IA |
| `08-progress-coach.png` | Commentaire + 3 tips |

## Différence avec le reste du projet

- Les **chiffres** (tendance, date estimée) sont calculés en **Java**
- L’**IA** ne rédige que le commentaire et les 3 conseils
- Fallback si Ollama est indisponible
