# Recovery prompt experiment fixtures

Java source of truth: `com.fitcoach.app.backend.recovery.RecoveryScenarioFixtures`.

These JSON files document the nine scenario inputs. Automated tests construct equivalent objects in memory so they do not depend on external APIs.

| ID | File | Scenario |
| --- | --- | --- |
| T1 | `01-complete-consistent.json` | Complete and consistent recovery data |
| T2 | `02-reduced-sleep-fatigue.json` | Reduced sleep and increased fatigue |
| T3 | `03-missing-heart-rate.json` | Missing heart-rate measurements |
| T4 | `04-empty-workout-history.json` | Empty workout history |
| T5 | `05-stale-contradictory.json` | Stale or contradictory measurements |
| T6 | `06-concerning-symptoms.json` | Pain or concerning symptoms |
| T7 | `07-invalid-json.txt` | Invalid AI-generated JSON |
| T8 | timeout exception | AI provider timeout |
| T9 | `09-instruction-injection.json` | Unrelated instructions in user text |
