# Changelog

Les changements notables de ce projet sont consignés dans ce fichier.

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/), et les versions respectent [Semantic Versioning](https://semver.org/lang/fr/).

## [1.0.0] - 2026-09-25

### Ajouté

- Ouverture et clôture des sessions avec code de présence à durée limitée.
- Marquage de présence par les étudiants et prise en charge des requêtes concurrentes.
- Dépôt d’un lien d’exercice et affectation de deux relecteurs distincts parmi les étudiants présents.
- Notes provisoires après le premier avis et moyenne après le second ; commentaires anonymisés pour l’auteur.
- Espaces formateur, étudiant et relecteur, avec historique des relectures rendues.
- Tableau récapitulatif par étudiant et données de démonstration.
- Contrat OpenAPI, migrations Flyway et tests automatisés sur H2.

### Corrigé

- Les marquages de présence simultanés d’une même session sont sérialisés pour éviter la perte d’une présence.

### Hors périmètre de cette version

- La présence ajoutée manuellement par le formateur (issue [#7](https://github.com/steph0502/kfokam48-epreuve-258/issues/7), Should).
- Le remplacement du lien d’un exercice (issue [#8](https://github.com/steph0502/kfokam48-epreuve-258/issues/8), Should).
- Le blocage après cinq codes erronés (issue [#11](https://github.com/steph0502/kfokam48-epreuve-258/issues/11), Could).
