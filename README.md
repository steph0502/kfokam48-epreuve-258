# KFOKAM48 — Présences & Relecture par les pairs

Application pour la direction de la formation KFOKAM48 : le formateur ouvre une
session de cours et obtient un code de présence, les étudiants marquent leur
présence avec ce code, déposent le lien de leur exercice, et un pair désigné
relit l'exercice (note /20 + commentaire). Le formateur suit le tout dans un
tableau récapitulatif.

## Stack

| Couche | Techno | Dossier |
|---|---|---|
| Backend | Java 21 · Spring Boot · Maven (`mvnw`) · Flyway | `backend/` |
| Frontend | Next.js (React) | `frontend/` |
| Contrat d'API | OpenAPI | `api/contrat.yaml` |
| Analyse | Cahier des charges, diagrammes Mermaid, journal | `docs/` |

## Frontend choisi : Next.js

Routing par fichier intégré, rendu hybride SSR/CSR et un seul outil de build :
le trio d'écrans (formateur, étudiant, relecteur) s'assemble vite sans
configuration de routing ni de bundler.

## Démarrage

À compléter à l'étape 4 (testé depuis un clone vierge).
