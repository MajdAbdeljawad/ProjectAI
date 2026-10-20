# Contribution : suivi de progression et récupération

Module autonome importé du projet de suivi sportif. Il n'est pas encore raccordé à
l'authentification ni aux pages de FitCoach situées à la racine du dépôt.
Les applications du groupe restent inchangées.

## Fonctionnalités

- Objectifs : création, liste, modification, suppression, cible, unité, dates, statut.
- Progression : date et mesures facultatives de poids, tour de taille, sommeil, énergie de 1 à 5.
- Validation des entrées et erreurs `application/problem+json`.
- Persistance MariaDB ; Hibernate en mode `validate`, initialisation SQL désactivée.
- Angular sur 4200, API sur 8081. Identifiant utilisateur numérique, sans authentification.

## Lancement local

Prérequis : JDK 21 (compilation cible Java 17), Maven, Node compatible Angular 21,
npm et MariaDB (par exemple XAMPP).

Sur une base **neuve et vide uniquement**, créer une base dédiée `sport_tracking`,
un compte applicatif limité à cette base, puis appliquer
`src/main/resources/db/schema-mysql.sql`. Ne pas appliquer ce script à la base
FitCoach existante. Le script PostgreSQL est conservé comme historique.

Variables du processus backend :

| Variable | Défaut local |
| --- | --- |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3306` |
| `DB_NAME` | `sport_tracking` |
| `DB_USER` | `sport_app` |
| `DB_PASSWORD` | vide, uniquement pour le développement local |
| `SERVER_PORT` | `8081` |

Configurer les éventuels secrets localement, jamais dans Git.

Depuis ce dossier :

```sh
mvn clean -DskipTests compile
mvn spring-boot:run -DskipTests
```

Dans un second terminal :

```sh
cd frontend
npm ci
npm run build
npm start
```

Ne pas lancer une deuxième application sur un port déjà occupé. Le frontend du
groupe utilise aussi 4200 : lancer une seule interface à la fois. Le CORS de ce
module autorise uniquement `http://localhost:4200`. L'URL API est configurée dans
`frontend/src/environments/environment.ts`.

## Routes

Préfixe : `/api/users/{userId}`, avec `userId` entier positif.

| Méthode | Route relative | Action |
| --- | --- | --- |
| GET, POST | `/goals` | Lister, créer |
| GET, PUT, DELETE | `/goals/{goalId}` | Consulter, modifier, supprimer |
| GET, POST | `/progress-entries` | Lister, créer |
| GET, PUT, DELETE | `/progress-entries/{entryId}` | Consulter, modifier, supprimer |

Pages Angular : `/objectifs` et `/progression`.
Le changement de `userId` est un outil de développement, pas une protection d'accès.

## Intégration au groupe et IA

FitCoach possède déjà ses propres contrôleurs Goal/Progress/Recovery, ses migrations
Flyway, sa sécurité et une intégration Ollama. Cet import conserve la contribution
séparément pour revue : il ne remplace aucun de ces composants.

Avant fusion fonctionnelle, il faudra adapter les DTO et les appels à l'utilisateur
authentifié du groupe, convenir du modèle de données et réutiliser le fournisseur IA
existant. Ne pas copier les tables directement dans les migrations du groupe.

La première option proposée est un résumé de progression via Ollama local, à la
demande de l'utilisateur. Aucune nouvelle intégration IA n'est activée dans ce module.
Le choix du fournisseur doit être confirmé avant envoi de mesures à un service externe.

## Vérification

Le module local d'origine avait été compilé et vérifié manuellement le 19 octobre
2026 (CRUD, validation et persistance). Cette information n'est pas une validation
de l'intégration FitCoach. Aucun test automatisé n'est exécuté pour cet import.
