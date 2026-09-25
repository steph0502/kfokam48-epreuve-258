# D4 (bonus) — États-transitions du cycle de vie d'un exercice

> Bonus +3 du sujet. Le statut est porté par `Exercice.statut` (voir D2).

```mermaid
stateDiagram-v2
    [*] --> EN_ATTENTE : POST /api/exercices → 201 (EF3)

    EN_ATTENTE --> EN_ATTENTE : lien remplacé (EF9, RG11, Q13)
    EN_ATTENTE --> ASSIGNE : relecteur assigné parmi les présents ≠ auteur (EF4, RG6)

    note right of EN_ATTENTE
        Aucun autre étudiant présent (H3) :
        l'exercice reste ici, sans relecteur.
        Nouvelle tentative d'assignation
        à chaque présence ajoutée (RG14).
    end note

    ASSIGNE --> ASSIGNE : lien remplacé (EF9, RG11 — la relecture n'est pas rendue)
    ASSIGNE --> RELU : POST /api/relectures/{id} → 200 (EF5, note entière 0-20)

    note right of ASSIGNE
        Contrôles au rendu :
        403 AUTO_RELECTURE (RG4)
        400 NOTE_INVALIDE (RG7)
    end note

    RELU --> [*] : note définitive, pas de correction (RG8, Q15)

    note right of RELU
        Le relecteur relu voit la note et
        le commentaire, sans le nom du
        relecteur (RG13, Q8).
        Le lien n'est plus remplaçable (RG11).
    end note
```

**Règles associées :**

- La clôture de la session (EF7) ne change pas le statut d'un exercice : un exercice `EN_ATTENTE` ou `ASSIGNE` reste visible comme tel dans le tableau (Q11, RG9) via `relecturesEnAttente`.
- Transition impossible par construction : `RELU → ASSIGNE` ou `RELU → EN_ATTENTE` — la relecture rendue est définitive (RG8), le contrat renvoie `409 RELECTURE_DEJA_RENDUE`.
