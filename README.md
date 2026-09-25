# KFOKAM48 — Présences et relecture par les pairs

Application web pour suivre les présences et faire évaluer les exercices par deux pairs distincts. Le formateur ouvre une session et affiche son code de présence. Les étudiants pointent et déposent le lien de leur exercice. Chaque relecture rend une note sur 20 et un commentaire : la note du premier avis est provisoire, puis les deux notes sont moyennées lorsque le second avis est rendu.

## Fonctionnalités livrées

- Le formateur ouvre et clôture une session, affiche le code de présence et consulte le tableau récapitulatif.
- L’étudiant choisit son nom, marque sa présence, dépose son lien et consulte les avis anonymisés reçus.
- Le relecteur consulte ses affectations, rend une note et un commentaire pour chacune, puis retrouve les tâches rendues dans son historique.
- Les notes et moyennes sont calculées par l’API. Les erreurs API suivent le format `{code, message}`.

## Technologies

| Couche | Technologie | Dossier |
|---|---|---|
| Backend | Java 21, Spring Boot 4, Maven, Flyway | `backend/` |
| Frontend | Next.js 16, React 19, TypeScript | `frontend/` |
| Base de données | PostgreSQL 16 | `docker-compose.yml` |
| Contrat API | OpenAPI 3 | `api/contrat.yaml` |

**Pourquoi Next.js ?** Son routage par fichiers et son rendu hybride permettent de construire les trois espaces sans ajouter de bibliothèque de routage.

## Prérequis

- Docker avec Docker Compose v2
- JDK 21
- Node.js 20.9 ou supérieur et npm

## Démarrage depuis un clone

Les trois commandes ci-dessous se lancent dans trois terminaux depuis la racine du dépôt. La première attend que PostgreSQL soit prêt.

**Terminal 1 — base de données :**

```bash
docker compose up -d --wait
```

**Terminal 2 — API :**

```bash
cd backend && ./mvnw spring-boot:run
```

L’API répond sur <http://localhost:8080> et se connecte à PostgreSQL sur le port `5436`.

**Terminal 3 — interface :**

```bash
cd frontend && npm ci && npm run dev
```

L’application est disponible sur <http://localhost:3000>. Par défaut, elle appelle l’API sur <http://localhost:8080>. Pour utiliser une autre adresse d’API, définir `NEXT_PUBLIC_API_URL` avant de lancer Next.js.

## Données de démonstration

Flyway crée la promotion « KFOKAM48 — Promotion 2026 » et quatre étudiants. Sur une base sans session, le backend crée aussi une session de démonstration et écrit son code dans les logs ; ce code expire après 15 minutes. Depuis l’espace formateur, il est possible d’ouvrir une nouvelle session et d’obtenir un nouveau code. Choisir un étudiant dans l’espace étudiant pour pointer et déposer un lien. L’espace relecteur permet ensuite de rendre les avis ; l’étudiant relu voit les commentaires sans connaître l’identité des pairs.

## Vérifications locales

```bash
cd backend && ./mvnw test
```

```bash
cd frontend && npm ci && npm run build
```

## Documentation et backlog

- Contrat : [`api/contrat.yaml`](api/contrat.yaml)
- Cahier des charges et journal : [`docs/CAHIER_DES_CHARGES.md`](docs/CAHIER_DES_CHARGES.md), [`docs/JOURNAL.md`](docs/JOURNAL.md)
- Diagrammes : [`docs/diagrammes/`](docs/diagrammes/)
- Historique des changements : [`CHANGELOG.md`](CHANGELOG.md)

Issues restant ouvertes après l’étape 3 :

| Priorité | Issue | Sujet |
|---|---|---|
| Should | [#7](https://github.com/steph0502/kfokam48-epreuve-258/issues/7) | Ajouter une présence manuellement comme formateur |
| Should | [#8](https://github.com/steph0502/kfokam48-epreuve-258/issues/8) | Remplacer le lien d’un exercice |
| Could | [#11](https://github.com/steph0502/kfokam48-epreuve-258/issues/11) | Bloquer après cinq codes erronés pendant deux minutes |
