# Journal de bord — 258

> Une entrée **par étape**, écrite **au moment où tu la termines**, pas à la fin de la journée.
> Trois lignes suffisent. Un journal rédigé d'un bloc juste avant de soumettre se repère
> immédiatement dans l'historique Git et ne compte pas.

Chaque entrée répond aux trois mêmes questions :

- **Fait** — ce que tu viens de terminer
- **Bloqué** — ce qui t'a coûté du temps, et combien
- **IA** — ce que tu lui as demandé, et **comment tu as vérifié sa réponse**

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges (12 EF, 6 ENF, 18 RG numérotées avec sources Qx), 4 diagrammes Mermaid (D1 cas d'usage, D2 classes, D3 séquence « marquer sa présence », bonus D4 états d'un exercice), 15 issues créées avec critères d'acceptation et labels Must/Should/Could, contrat d'API complété (5 opérations imposées intactes + 6 ajoutées), squelette du dépôt posé. La contradiction Q10/Q15 est documentée comme hypothèse H9, sans code de modification. Jalon `[JALON] analyse` poussé.

**Bloqué :** ~25 min sur le tranchage Q10/Q15 : Q10 autorise le relecteur à corriger sa note jusqu'à la clôture, tandis que Q15 la déclare définitive dès l'envoi. Q10 est désormais prioritaire : le `409 RELECTURE_DEJA_RENDUE` est interprété comme le refus d'une seconde création par le POST imposé, pas comme l'interdiction absolue d'une opération de correction distincte. L'opération de correction et l'identité du relecteur restent à valider (H9) ; aucun code de modification ne sera écrit avant cette confirmation. Trou repéré aussi : le blocage après 5 erreurs (Q4) n'a aucun endpoint dans le contrat (hypothèse H1, priorité Could), et un étudiant seul présent rend l'assignation impossible (hypothèse H3 → RG14).

**IA :** l'IA a produit le premier jet du cahier des charges, des diagrammes, du contrat complété et du découpage en issues. Vérifié en relisant chaque exigence et chaque règle contre CLIENT.md (chaque RG cite sa source Qx), en confrontant Q10/Q15 au contrat imposé et au modèle de l'épreuve, en contrôlant la cohérence D2 ↔ migrations prévues et D3 ↔ codes HTTP du contrat, et en cherchant moi-même contradictions et trous avant de comparer avec ce que l'IA en disait.

---

## Étape 2 — Première version

**Fait :** les 9 tickets Must livrés, un par branche et une PR par ticket (#12, #1 à #6 backend, #13/#14 frontend), chacun fermé par son merge. Backend : Spring Boot 4 + Flyway V1/V2, erreurs centralisées au format imposé, les 5 opérations du contrat + 5 ajoutées (clôture, présence formateur, lien, étudiants, correction PUT), 57 tests verts (unitaires + intégration sur H2, sans base locale). Frontend : Next.js, couche API dédiée, écrans formateur et étudiant. Tranchage H9 posé à l'étape 1 appliqué après validation : correction via PUT + identité par ?etudiantId=. Jalon v0.1 posé.

**Bloqué :** deux vrais bugs attrapés par les tests : le seed V2 avec ids explicites ne faisait pas avancer les séquences IDENTITY (collision garantie au premier INSERT sur un clone vierge) — corrigé en insérant sans ids ; et @Max(20) sur un DTO court-circuitait RG7 avec le mauvais code d'erreur — règle rendue à sa source unique dans le service. ~30 min à chaque fois, mais les tests d'intégration sur scénario réel (EF1→EF5) ont prouvé le reste.

**IA :** l'IA a écrit les services, contrôleurs, migrations et écrans sous mon découpage en tickets et mes validations (commit, push, merge approuvés un par un). Vérifié en relisant chaque règle contre le CDC (codes HTTP du contrat, RG citées dans les tests), en exigeant des tests d'intégration sur scénario réel plutôt que des mocks seuls, et en traquant deux défauts qu'elle avait elle-même introduits (séquences, borne DTO). Les décisions de conception (H9, H10, tri des sessions de test) ont été tranchées par moi, pas par elle.

---

## Étape 3 — Enveloppe

**Fait :** issue #25 créée avant le correctif ; test d’intégration reproduisant le rollback de présence sous concurrence, puis verrou pessimiste de session et suite backend de l’étape 2 vérifiée. Issue #26 créée pour le Must tardif : deux relecteurs distincts par exercice, note provisoire après un avis, moyenne après deux. Migration Flyway V3 conserve chaque ancienne affectation comme relecteur 1. API, CDC et diagrammes mis à jour ; écrans étudiant, formateur et relecteur intégrés au design local existant.

**Bloqué :** un test a révélé une liste non modifiable pendant la seconde affectation ; corrigé avant validation. Vérification finale : suite backend verte et build frontend Next.js vert.

**IA :** l’IA a assisté le diagnostic du défaut de concurrence, l’implémentation, les tests et la mise à jour documentaire. Vérifié avec le test concurrent qui échouait avant correctif, les tests d’intégration double avis et conservation de migration, `mvn test` (61 tests, 0 échec) et `npm run build`.

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :** EF12 / issue #11 reste ouverte et différée, priorité Could. La capacité a été réservée au Must tardif des deux relecteurs et des notes provisoires.

---

## Étape 4 — Version finale

**Fait :** synchronisation de `main` après la fusion de la PR #28. README racine et guide frontend actualisés ; `CHANGELOG.md` préparé au format Keep a Changelog pour la v1.0.0. Backlog restant vérifié sur GitHub : #7 et #8 Should, #11 Could. Test depuis un clone temporaire propre : Flyway applique V1–V3, l’API démarre et retourne les quatre étudiants de démonstration, la route `/relecteur` répond 200.

**Bloqué :** le port 5436 et le nom du conteneur PostgreSQL étaient déjà utilisés sur le poste ; le clone de test a été isolé sur le port 55436, sans toucher au service existant. Le port 3000 étant également occupé, le serveur Next de test a utilisé le port 43000.

**IA :** l’IA a mis à jour le guide et le changelog après inspection de la configuration réelle. Vérifications : critères de l’issue #15 et labels des issues ouvertes relus sur GitHub ; commandes vérifiées depuis le clone temporaire (`npm ci`, lancement Next, démarrage Spring, endpoint étudiants et `/relecteur`). Aucun commit ni push effectué.

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
