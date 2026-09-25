# Journal de bord — KF48-STP-001 (matricule 258)

> Une entrée **par étape**, écrite **au moment où tu la termines**, pas à la fin de la journée.
> Trois lignes suffisent. Un journal rédigé d'un bloc juste avant de soumettre se repère
> immédiatement dans l'historique Git et ne compte pas.

Chaque entrée répond aux trois mêmes questions :

- **Fait** — ce que tu viens de terminer
- **Bloqué** — ce qui t'a coûté du temps, et combien
- **IA** — ce que tu lui as demandé, et **comment tu as vérifié sa réponse**

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges (12 EF, 6 ENF, 18 RG numérotées avec sources Qx), 4 diagrammes Mermaid (D1 cas d'usage, D2 classes, D3 séquence « marquer sa présence », bonus D4 états d'un exercice), 15 issues créées avec critères d'acceptation et labels Must/Should/Could, contrat d'API complété (5 opérations imposées intactes + 6 ajoutées), squelette du dépôt posé. Jalon `[JALON] analyse` poussé.

**Bloqué :** ~25 min sur le tranchage Q10/Q15 : Q10 autorise le relecteur à corriger sa note, Q15 la déclare définitive une fois envoyée. Tranché en faveur de **Q15 (RG8)** : le contrat d'API imposé renvoie déjà `409 RELECTURE_DEJA_RENDUE`, le contredire coûterait les points de conformité — notez que le modèle d'entrée du sujet tranche l'inverse, mon argumentaire est dans la section 7 du cahier des charges. Trou repéré aussi : le blocage après 5 erreurs (Q4) n'a aucun endpoint dans le contrat (hypothèse H1, priorité Could), et un étudiant seul présent rend l'assignation impossible (hypothèse H3 → RG14).

**IA :** l'IA a produit le premier jet du cahier des charges, des diagrammes, du contrat complété et du découpage en issues. Vérifié en relisant chaque exigence et chaque règle contre CLIENT.md (chaque RG cite sa source Qx), en contrôlant la cohérence D2 ↔ migrations prévues et D3 ↔ codes HTTP du contrat, et en cherchant moi-même contradictions et trous avant de comparer avec ce que l'IA en disait.

---

## Étape 2 — Première version

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 3 — Enveloppe

**Fait :**

**Bloqué :**

**IA :**

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

---

## Étape 4 — Version finale

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
