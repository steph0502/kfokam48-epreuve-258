# D2 — Modèle de données (classes)

> Ce diagramme est le modèle cible ; il devra correspondre à la migration Flyway `V1__init.sql`, qui sera créée à l'étape suivante.
> Toute évolution du schéma à l'étape 3 se fera dans une **nouvelle migration**
> `V2__...` et ce diagramme sera mis à jour dans le même commit.

```mermaid
classDiagram
    class Promotion {
        +Long id
        +String nom
    }

    class Etudiant {
        +Long id
        +String nom
        +Long promotionId
    }

    class Session {
        +Long id
        +String titre
        +Long promotionId
        +String code
        +Instant ouvertureAt
        +Instant expirationAt
        +Instant clotureAt
    }

    class Presence {
        +Long id
        +Long sessionId
        +Long etudiantId
        +String source
        +Instant creeAt
    }

    class Exercice {
        +Long id
        +Long sessionId
        +Long etudiantId
        +String lien
        +String statut
        +Instant deposeAt
    }

    class Relecture {
        +Long id
        +Long exerciceId
        +Long relecteurId
        +Integer note
        +String commentaire
        +Instant rendueAt
    }

    class TentativeCode {
        +Long id
        +Long sessionId
        +Long etudiantId
        +Instant echecAt
    }

    Promotion "1" --> "1..*" Etudiant : regroupe
    Promotion "1" --> "1..*" Session : accueille
    Session "1" --> "0..*" Presence : enregistre
    Etudiant "1" --> "0..*" Presence : pointe
    Session "1" --> "0..*" Exercice : reçoit
    Etudiant "1" --> "0..*" Exercice : dépose (auteur)
    Exercice "1" --> "0..1" Relecture : fait l'objet de
    Etudiant "1" --> "0..*" Relecture : effectue (relecteur)
    Session "1" --> "0..*" TentativeCode : trace
    Etudiant "1" --> "0..*" TentativeCode : échoue
```

**Notes de conception :**

- **Pas d'entité Relecteur** : le relecteur est un `Etudiant` référencé par `Relecture.relecteurId` (décision §2 du cahier des charges, Q7).
- `Presence.source` ∈ `{ETUDIANT, FORMATEUR}` (Q14, champ imposé par le contrat).
- `Exercice.statut` ∈ `{EN_ATTENTE, ASSIGNE, RELU}` — cycle de vie détaillé dans D4.
- `Exercice.etudiantId` = l'**auteur** ; `Relecture.relecteurId` = celui qui relit. Un étudiant apparaît donc dans les deux rôles.
- `Session.clotureAt` est `null` tant que le formateur n'a pas clôturé (Q3, Q12).
- `TentativeCode` alimente le blocage de 2 minutes après 5 échecs (Q4, RG3) — exigence **Could** (EF12).
- Contraintes d'unicité : `(session_id, etudiant_id)` unique sur `presence` (RG16) et sur `exercice` (RG17) ; `code` unique parmi les sessions non expirées (RG18).
- **Q10/H9 tranché** : la correction met à jour la même `Relecture` (pas d'historique de versions) ; aucune colonne supplémentaire n'était nécessaire, `rendue_at` conserve la date de la création initiale.
