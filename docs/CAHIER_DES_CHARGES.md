# Cahier des charges — KFOKAM48 Présences & Relectures

**Auteur :** Stephane TANKOUA · **Matricule : 258**
**Version :** 1 · **Date :** 25/09/2026
**Frontend choisi :** **Next.js**, parce que le routing par fichier et un seul outillage de build permettent d'assembler les trois écrans imposés (formateur, étudiant, relecteur) sans perdre de temps sur la configuration.

> Chaque décision prise « à la place du client » cite sa source : une question (`Qx`) de `CLIENT.md`, une règle du contrat d'API, ou une hypothèse (`Hx`) définie en section 7.

---

## 1. Contexte et objectif

La direction de la formation KFOKAM48 suit aujourd'hui les présences et les exercices de ses étudiants à la main : appel oral, feuille de présence, liens d'exercices reçus par messagerie, corrections distribuées sans traçabilité. Le formateur n'a aucune vue consolidée : qui était là, qui a rendu, qui a relu quoi, et avec quelle note.

L'application répond à ce problème avec un workflow simple en cinq temps : le formateur ouvre une session de cours et reçoit un **code de présence** éphémère ; chaque étudiant saisit ce code depuis son téléphone pour pointer ; l'étudiant dépose ensuite le **lien** de son exercice pour la session ; le système **désigne au hasard un pair** présent pour relire l'exercice et leur attribuer une note sur 20 avec un commentaire ; un seul avis donne une note provisoire et deux avis sont moyennés ; enfin, le formateur consulte un **tableau récapitulatif** par étudiant (présences, exercices déposés, moyenne des notes reçues, relectures en attente).

L'objectif de cette version : remplacer le papier par une application web simple, utilisable en séance depuis un téléphone, **sans compte ni mot de passe** (Q1), livrée avec des données de démonstration pour être vérifiable immédiatement.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session (obtenir le code), clôturer une session, ajouter une présence à la main (Q14), consulter le tableau récapitulatif (Q16) | Marquer sa propre présence, déposer un exercice, relire, modifier une note |
| **Étudiant** | Choisir son nom dans la liste (Q1), marquer sa présence avec le code, déposer le lien de son exercice, remplacer son lien avant relecture rendue (Q13), consulter la note et le commentaire reçus (Q8) | Marquer sa présence deux fois, déposer deux exercices pour une même session, voir le nom de son relecteur (Q8) |
| **Relecteur** | Rendre une ou deux relectures distinctes : note entière 0–20 + commentaire (Q9), sur l'exercice qui lui est assigné ; corriger sa note et son commentaire tant que la session n'est pas clôturée (Q10, H9, décision provisoire) | Relire son propre exercice (Q5), soumettre une seconde création de relecture (409 du contrat), corriger après la clôture de la session (H9), voir les relectures des autres |

**Décision structurante :** le relecteur **n'est pas un acteur distinct**. C'est un étudiant auquel le système a temporairement assigné un exercice (Q7 : désigné « parmi les étudiants présents »). L'enveloppe permet deux relecteurs distincts pour un exercice. Conséquence sur le modèle de données : pas d'entité `Relecteur` ; chaque entité `Relecture` porte une référence vers `Etudiant` (celui qui relit) et vers `Exercice` (celui qui est relu). Le formulaire de relecture est simplement une vue accessible à l'étudiant qui a une assignation en attente.

## 3. Périmètre

**Inclus dans cette version :**

- Ouverture/clôture de sessions de cours avec code de présence expirant (Q2)
- Marquage de présence par code, unicité par étudiant et par session
- Présence ajoutée manuellement par le formateur, distinguée par `source=FORMATEUR` (Q14)
- Dépôt du **lien** d'un exercice par session (pas de fichier), remplaçable avant relecture rendue (Q13)
- Assignation aléatoire d'un relecteur parmi les présents, auteur exclu (Q5, Q7)
- Rendu de deux relectures : notes entières 0–20 + commentaires (Q9) ; note retenue égale à leur moyenne, provisoire après un seul avis (enveloppe étape 3)
- Tableau récapitulatif du formateur par promotion (Q16), moyenne calculée côté API
- Écrans : formateur (session + tableau), étudiant (présence + dépôt), relecteur (relecture)
- Migrations de schéma versionnées (Flyway), données de démonstration, `docker compose up`

**Explicitement exclu :**

- **Authentification et mots de passe** — l'étudiant choisit son nom dans une liste (Q1). Le formateur n'est pas non plus authentifié : c'est un choix assumé, cohérent avec le contexte mono-formateur de la demande.
- **Upload de fichiers** — un exercice est un lien (URL), jamais un binaire.
- **Notifications** (email, push) — l'étudiant relu consulte sa note dans l'application.
- **Temps réel** (WebSocket) — le tableau et les listes se rafraîchissent à la demande.
- **CRUD de promotions et d'étudiants** — ils existent comme données de démonstration, pas d'écran de gestion.
- **Multi-formateur et gestion fine des rôles**, historisation des modifications de notes, i18n (français uniquement), responsive au-delà des écrans de présence et du tableau.

*Ce que j'exclus compte autant que ce que j'inclus : ces exclusions sont ce qui rend la version livrable dans le temps imparti sans sacrifier les Must.*

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session et obtient un code de présence | `POST /api/sessions {titre, promotionId}` renvoie `201 {id, code, ouvertureAt, expirationAt}` avec `expirationAt = ouvertureAt + 15 min` (RG1) | Must |
| EF2 | L'étudiant marque sa présence avec le code | `POST /api/presences {code, etudiantId}` renvoie `201 {id, sessionId, etudiantId, source=ETUDIANT}` ; la présence apparaît dans le tableau. Erreurs : `400 CODE_INCONNU`, `409 DEJA_PRESENT` (RG16), `410 CODE_EXPIRE` (RG1) | Must |
| EF3 | L'étudiant dépose le lien de son exercice | `POST /api/exercices {sessionId, etudiantId, lien}` renvoie `201 {id, statut}` ; un second dépôt pour la même session renvoie `409 EXERCICE_DEJA_DEPOSE` (RG17) | Must |
| EF4 | Le système assigne deux relecteurs au hasard | Dès que possible, deux étudiants présents, distincts de l’auteur et l’un de l’autre (RG4–RG6), sont affectés. Les affectations manquantes sont retentées à chaque nouvelle présence (RG14). L’exercice reste `EN_ATTENTE` sans avis affecté et passe à `ASSIGNE` après au moins une affectation | Must |
| EF5 | Le relecteur rend sa note et son commentaire | `POST /api/relectures/{id} {note, commentaire}` renvoie `200` pour la création initiale ; `note` entière hors 0–20 → `400 NOTE_INVALIDE` (RG7) ; relecture de son propre exercice → `403 AUTO_RELECTURE` (RG4) ; une seconde soumission → `409 RELECTURE_DEJA_RENDUE` (RG8). La correction avant clôture (Q10) est portée par `PUT /api/relectures/{id}` (H9 tranché, identité par `?etudiantId=`) ; chaque affectation est rendue séparément. Après un seul rendu, la note est provisoire ; après deux, la note retenue est leur moyenne ; la soumission initiale reste possible après clôture (H10, RG19). | Must |
| EF6 | Le formateur voit le tableau récapitulatif | `GET /api/tableau?promotionId=` renvoie `200` : par étudiant, `presences`, `exercicesDeposes`, `moyenne` (null si aucune note, RG15), `moyenneProvisoire`, `relecturesEnAttente` ; promotion inconnue → `404 PROMOTION_INCONNUE` | Must |
| EF7 | Le formateur clôture la session | Opération ajoutée au contrat (`POST /api/sessions/{id}/cloture`) ; après clôture : présence refusée (RG2) et dépôt refusé (RG10), l'exercice en attente reste visible comme tel (RG9) | Must |
| EF8 | Le formateur ajoute une présence à la main | Opération ajoutée (`POST /api/sessions/{id}/presences`) ; la présence est enregistrée avec `source=FORMATEUR` et visible comme telle dans le tableau (Q14, RG12) | Should |
| EF9 | L'étudiant remplace le lien de son exercice | Opération ajoutée (`PUT /api/exercices/{id}/lien`) ; accepté tant que la relecture n'est pas rendue (RG11) ; refusé après → `409` | Should |
| EF10 | L’étudiant relu consulte ses notes et commentaires | L’étudiant voit les avis rendus, la note retenue et son caractère provisoire si un seul avis existe, sans identité des relecteurs (RG13, RG19) | Must (étape 3) |
| EF11 | L'étudiant choisit son nom dans une liste | Opération ajoutée (`GET /api/etudiants?promotionId=`) renvoyant la liste des étudiants de la promotion (Q1) | Should |
| EF12 | Le système bloque l’étudiant après 5 codes erronés | 5 `CODE_INCONNU` consécutifs → refus `400 LIMITE_TENTATIVES` pendant 2 minutes ; **non livré à l’étape 3, issue #11 maintenue ouverte au statut Could** | Could |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | L'écran de saisie du code de présence est utilisable sur un téléphone en 3 gestes maximum | Test manuel sur viewport 375 px : champ code + bouton, sans scroll horizontal |
| ENF2 | Le tableau du formateur, avec indicateurs de moyenne provisoire, répond en moins de 2 s pour une promotion de 60 étudiants | Requête chronométrée sur le jeu de démonstration (60 étudiants, 3 sessions) |
| ENF3 | Aucune erreur API ne renvoie de stack trace : format `{code, message}` partout | Un appel erroné quelconque renvoie toujours le format imposé — vérifié par tests d'intégration |
| ENF4 | L'application démarre chez un tiers avec `docker compose up` (ou 3 commandes max) et des données de démonstration | Test du README depuis un clone vierge dans un dossier vide |
| ENF5 | Les tests tournent sur un poste vierge, sans base installée | `mvn test` passe avec base en mémoire (H2) et migrations Flyway jouées — aucun prérequis local |
| ENF6 | Le temps de réponse nominal des endpoints est < 500 ms | Requêtes simples indexées ; constaté sur le jeu de démonstration |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Le code de présence expire 15 minutes après l'ouverture de la session ; après, toute tentative renvoie `410 CODE_EXPIRE` | Q2 · contrat |
| RG2 | Aucune présence après la clôture de la session | Q3 |
| RG3 | À terme, après 5 codes erronés consécutifs, l’étudiant est bloqué 2 minutes ; règle non livrée, suivie par EF12 / issue #11 (Could) | Q4
| RG4 | Un étudiant ne peut jamais relire son propre exercice → `403 AUTO_RELECTURE` | Q5 · contrat |
| RG5 | Chaque exercice peut avoir deux relecteurs distincts au maximum ; l’objectif nominal est deux avis | Q6 remplacée par l’enveloppe de l’étape 3 |
| RG6 | Les deux relecteurs sont choisis au hasard parmi les étudiants **présents à cette session**, l’auteur et le premier relecteur étant exclus des candidats du second | Q7 · enveloppe étape 3 |
| RG7 | Une note est un **entier** compris entre 0 et 20, sinon `400 NOTE_INVALIDE` | Q9 · contrat |
| RG8 | Une seconde création de relecture renvoie `409 RELECTURE_DEJA_RENDUE`. Tant que la session n'est pas clôturée, la version courante peut être corrigée par le relecteur assigné via `PUT /api/relectures/{id}` (Q10, H9 tranché) ; après clôture, elle est définitive (Q15, `409 CORRECTION_INTERDITE`). La soumission initiale d'une relecture assignée reste possible après clôture (H10). | Q10 + Q15 · contrat |
| RG9 | Un exercice non relu reste « en attente » et apparaît comme tel dans le tableau (`relecturesEnAttente`) | Q11 |
| RG10 | Le dépôt d'exercice est possible jusqu'à la clôture de la session, pas après | Q12 |
| RG11 | Le lien d'un exercice est remplaçable tant que la relecture n'est pas rendue | Q13 |
| RG12 | Une présence ajoutée par le formateur porte `source=FORMATEUR`, les autres `source=ETUDIANT` | Q14 · contrat |
| RG13 | L'étudiant relu voit la note et le commentaire, jamais le nom du relecteur | Q8 |
| RG14 | Tant qu’il manque une ou deux affectations, l’exercice reste sans relecteur si aucune n’existe, sinon `ASSIGNE` ; le système retente les affectations manquantes à chaque nouvelle présence de la session | Hypothèse H3 · enveloppe étape 3 |
| RG15 | Pour chaque exercice, la note retenue est la moyenne des avis rendus (une seule note reste provisoire). La moyenne de l’étudiant est la moyenne des notes retenues de ses exercices, `null` sans aucune note ; elle est calculée par l’API, jamais par le frontend | Q16 · contrat · F3 · enveloppe étape 3 |
| RG16 | Un étudiant ne peut avoir qu'une présence par session → `409 DEJA_PRESENT` | Contrat |
| RG17 | Un étudiant ne peut déposer qu'un exercice par session → `409 EXERCICE_DEJA_DEPOSE` | Contrat |
| RG18 | Le code de présence est une chaîne de 6 caractères alphanumériques majuscules, générée par le backend, unique parmi les sessions ouvertes | Hypothèse H6 |
| RG19 | Une note d’exercice basée sur un seul avis rendu est provisoire ; avec deux avis rendus, la note retenue est leur moyenne arithmétique. Le formateur voit si sa moyenne contient au moins une note provisoire | Enveloppe étape 3

## 7. Zones d'ombre, hypothèses et contradictions

**Contradictions relevées :**

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10 vs Q15** — Q10 autorise le relecteur à corriger sa note tant que la session n'est pas clôturée ; Q15 déclare la note définitive une fois envoyée | **Q10, mis en œuvre à l'étape 2** | Q10 est la réponse la plus précise sur la fenêtre de modification. Le `409 RELECTURE_DEJA_RENDUE` du contrat vise une seconde création par `POST`, pas nécessairement une opération de correction distincte, puisque le sujet autorise les opérations API additionnelles. Q15 est conservée pour l'état final après clôture. **H9 tranché** : l'identité du relecteur transite par le paramètre de requête `etudiantId` (le corps imposé `{note, commentaire}` reste intact) et la correction est portée par `PUT /api/relectures/{id}`, fermée après clôture (`409 CORRECTION_INTERDITE`). |
| **Q2 vs Q3** — deux « fins » différentes : expiration du code (15 min) et clôture de session par le formateur | **Deux horizons distincts** : l'expiration tue le code (RG1), la clôture tue la session (RG2). Un dépôt reste possible après expiration du code (Q12), une présence non | Q12 démontre que « fin de session » au sens du client ne signifie pas expiration du code ; les deux règles gouvernent des opérations différentes |
| Q6 vs enveloppe étape 3 — un seul ou deux relecteurs | L’enveloppe tardive est prioritaire : deux relecteurs distincts ; une migration ajoute le second emplacement et conserve l’affectation existante comme relecteur 1 | Must accepté ; EF12 / issue #11 reste ouverte, priorité Could, reportée pour réserver la capacité au changement demandé |

**Points que la demande ne tranche pas (hypothèses) :**

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| H1 — Le blocage après 5 erreurs (Q4) n'a **aucun endpoint** dans le contrat imposé | Q4 + hypothèse | Non livré ; reporté avec EF12 / issue #11, priorité **Could** | Le contrat n'étant pas modifiable sur `/api/presences`, un statut dédié (429) aurait violé « codes de statut à la lettre » |
| H2 — « Commencé à relire » (Q13) : quand le lien devient-il non remplaçable ? | Hypothèse | « Commencé » = la relecture est **rendue** (note soumise). Avant cela, le lien est remplaçable, même après assignation du relecteur | Simple à vérifier : un seul état, pas de suivi de « saisie en cours » |
| H3 — Que se passe-t-il avec **un seul étudiant présent** ? L'assignation (Q7) est impossible puisque se relire est interdit (Q5) | Hypothèse | L'exercice reste `EN_ATTENTE` sans relecteur ; nouvelle tentative d'assignation à chaque présence ajoutée à la session (RG14) ; visible dans le tableau via `relecturesEnAttente` | Le formateur voit le cas et peut ajouter une présence à la main (EF8) pour débloquer |
| H4 — Dépôt d'exercice **après clôture** (Q12) : le contrat ne prévoit pas d'erreur dédiée sur `POST /api/exercices` | Hypothèse | Refus `400 SESSION_CLOTUREE`, format d'erreur imposé respecté | Identique à H1 : pas de code de statut supplémentaire |
| H5 — Présence **après clôture** avec un code encore temporellement valide | Q3 | Refus `400 SESSION_CLOTUREE` (et non 410, réservé à l'expiration, RG1) | Distinction claire entre « code expiré » et « session fermée » |
| H6 — Format et unicité du code de présence | Hypothèse | 6 caractères alphanumériques majuscules, générés par le backend, uniques parmi les sessions non expirées (RG18) | Suffisant pour une salle de classe, lisible sur un téléphone |
| H7 — Compteur d'erreurs (Q4) : portée et remise à zéro | Hypothèse | Compteur par (étudiant, session ouverte) ; remis à zéro après une présence réussie ou un blocage écoulé | Sans authentification, l'étudiantId est la seule clé disponible |
| H8 — Identification sans mot de passe (Q1) | Q1 | Aucune authentification ; l'étudiantId vient d'une liste choisie à l'écran ; le risque d'usurpation est accepté et **écrit ici** | Le client a explicitement écarté la connexion ; conséquence notée, pas cachée |
| H9 — Q10 autorise une correction avant clôture alors que Q15 rend la note définitive dès l'envoi | Q10 + Q15 | **Tranché** : la correction est portée par `PUT /api/relectures/{id}` avec `?etudiantId=` (identité du relecteur) ; le POST reste réservé à la création initiale. La version finale est gelée à la clôture (`409 CORRECTION_INTERDITE`). | Le corps du POST imposé reste strictement conforme ; un paramètre de requête additionnel est cohérent avec `GET /api/relectures?etudiantId=` |
| H10 — La demande ne dit pas si une relecture assignée peut encore être rendue après la clôture | Hypothèse | La soumission initiale reste possible après la clôture ; seule la correction est fermée (Q15) | Q12 tolère les dépôts tardifs et Q11 exige la visibilité des relectures en attente : la relecture d'un exercice assigné doit pouvoir aboutir, sinon l'étudiant relu perd sa note |


## 8. Contraintes techniques

**Imposées par le sujet :**

- **B1** Java 17+, Maven, wrapper `mvnw` commité
- **B2** Contrat `api/contrat.yaml` respecté à la lettre : chemins, verbes, codes de statut, format d'erreur
- **B3** Séparation contrôleur / service / repository ; aucune requête SQL dans un contrôleur ; aucune entité JPA exposée en JSON — passage systématique par des DTO
- **B4** Validation des entrées (Bean Validation) + gestion centralisée des erreurs via `@RestControllerAdvice` ; aucune stack trace au client
- **B5** Schéma versionné par **Flyway**, migrations commitées ; `ddl-auto=update` interdit hors tests
- **B6** Deux tests minimum : un test unitaire sur une règle métier réelle (ex. RG7 note hors bornes, RG4 auto-relecture), un test d'intégration sur un endpoint — exécutables sans base locale
- **F1** Next.js déclaré et justifié en une ligne dans le README ; le build passe
- **F2** Trois écrans : formateur (session + tableau), étudiant (présence + dépôt), relecteur (relecture)
- **F3** Appels API dans une couche dédiée, états de chargement et d'erreur gérés, **aucune règle métier dupliquée** (la moyenne vient de l'API)
- **Démarrage** `docker compose up` ou 3 commandes max, données de démonstration au démarrage

**Choisies par moi :**

- Base de données **PostgreSQL** en exécution (via docker compose), **H2 en mémoire** pour les tests — les migrations Flyway passent sur les deux, ce qui garantit B6 et un schéma identique dev/test
- Horodatages en **UTC** côté API, conversion d'affichage côté frontend
- Erreurs métier modélisées par des exceptions dédiées (`CodeExpireException`, `DejaPresentException`…) traduites en `{code, message}` par un seul handler

## 9. Livrables

- Dépôt GitHub public `kfokam48-epreuve-258`, structure `docs/` · `api/` · `backend/` · `frontend/`
- `docs/CAHIER_DES_CHARGES.md` (ce document) + `docs/JOURNAL.md` + `docs/diagrammes/` (D1, D2, D3, bonus D4)
- `api/contrat.yaml` : les 5 opérations imposées + les opérations ajoutées (clôture, présence manuelle, remplacement de lien, liste des étudiants)
- Backend Spring Boot (Java 21, Maven, Flyway, DTO, `@RestControllerAdvice`, tests)
- Frontend Next.js (3 écrans, couche API dédiée, états de chargement/erreur)
- `docker-compose.yml`, données de démonstration, `CHANGELOG.md`, README testé depuis un clone vierge
- Backlog en issues GitHub (critères d'acceptation, Must/Should/Could, renvois EFx/RGx)
- `SOUMISSION.md` téléversé sur la plateforme avant 18h00

## 10. Démarche prévue

1. **Étape 1 — Analyse (ce document, diagrammes, issues, contrat)** puis jalon `[JALON] analyse` — aucun code
2. **Étape 2 — v0.1** : stories Must uniquement, une branche par ticket, une PR par branche, issues fermées par les commits, migrations Flyway dès la première entité ; jalon `[JALON] v0.1`
3. **Étape 3 — Enveloppe** : issue ouverte **avant** de coder, bug reproduit, migration versionnée, contrat et analyse mis à jour, correctif et évolution dans des commits séparés
4. **Étape 4 — v1.0** : jalon `[JALON] v1.0`, `CHANGELOG.md`, README testé depuis un clone vierge, backlog trié
5. **Étape 5 — Épreuve Git** sur le dépôt séparé `kfokam48-gitlab-258`
6. **Étape 6 — Soumission** vérifiée en navigation privée, puis dépôt sur la plateforme

**Si je prends du retard :** je sacrifie dans l'ordre — EF12 / issue #11 (Could, maintenue ouverte et différée), puis les Should (EF8 → EF9 → EF11), jamais l'analyse ni l'hygiène Git, qui pèsent 70 points à elles deux.

**Definition of Done — un ticket est terminé quand :**

- Les critères d'acceptation de l'issue sont vérifiables et vérifiés (y compris les codes d'erreur)
- Le code est sur une branche dédiée, mergée par PR référençant l'issue (`Closes #n`)
- Les tests passent (`mvn test`), le build frontend passe
- Les règles RGx concernées sont citées dans le message de commit
- La documentation (cahier des charges, diagrammes, contrat) reste cohérente avec le code livré

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | 25/09/2026 | Version initiale |
| 2 | 25/09/2026 | Étape 3 : deux relecteurs, note provisoire puis moyenne, issue #11 explicitement différée (Could) pour absorber le Must tardif |
