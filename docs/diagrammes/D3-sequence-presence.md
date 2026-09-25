# D3 — Séquence : « marquer sa présence »

> Cas nominal **et** trois cas d'erreur, conformes aux codes HTTP du contrat :
> `201` nominal · `400 CODE_INCONNU` · `410 CODE_EXPIRE` (RG1) · `409 DEJA_PRESENT` (RG16).
> La clôture préalable de la session renvoie `400 SESSION_CLOTUREE` (RG2, hypothèse H5).

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Front (Next.js)
    participant C as PresenceController
    participant S as PresenceService
    participant R as SessionRepository
    participant X as ExerciceService

    E->>F: saisit le code + choisit son nom
    F->>C: POST /api/presences { code, etudiantId }

    alt code inconnu
        C->>S: enregistrer(code, etudiantId)
        S->>R: findByCode(code)
        R-->>S: null
        S-->>C: CodeInconnuException
        C-->>F: 400 { code: "CODE_INCONNU", message: "..." }
    else code expiré (RG1)
        C->>S: enregistrer(code, etudiantId)
        S->>R: findByCode(code)
        R-->>S: session (expirationAt < now)
        S-->>C: CodeExpireException
        C-->>F: 410 { code: "CODE_EXPIRE", message: "Le code de présence a expiré." }
    else déjà présent (RG16)
        C->>S: enregistrer(code, etudiantId)
        S->>R: existsBySessionIdAndEtudiantId(...)
        R-->>S: true
        S-->>C: DejaPresentException
        C-->>F: 409 { code: "DEJA_PRESENT", message: "..." }
    else session clôturée (RG2)
        C->>S: enregistrer(code, etudiantId)
        S-->>C: SessionClotureeException
        C-->>F: 400 { code: "SESSION_CLOTUREE", message: "..." }
    else cas nominal
        C->>S: enregistrer(code, etudiantId)
        S->>R: session valide, présence inexistante
        S->>S: save(Presence{ source: ETUDIANT })
        S-->>C: Presence créée
        C-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
        S->>X: retenterAssignations(sessionId)
        X->>X: compléter les affectations manquantes
        F-->>E: confirmation « présence enregistrée »
    end
```

**Points de cohérence vérifiés :**

- `findByCode` verrouille la session pendant l’enregistrement de présence ; après chaque nouvelle présence, les affectations manquantes des exercices non `RELU` sont complétées. Les deux relecteurs sont distincts de l’auteur et l’un de l’autre (RG4–RG6, RG14).

- Après chaque présence, `ExerciceService` complète les affectations manquantes des exercices non `RELU` ; le premier candidat ne peut être l’auteur, le second ne peut être ni l’auteur ni le premier relecteur (RG4–RG6, RG14).

- Les quatre erreurs passent toutes par le `@RestControllerAdvice` (B4) : format `{code, message}` garanti, aucune stack trace.
- L'ordre des vérifications dans le service : existence du code → expiration (RG1) → clôture (RG2) → unicité de la présence (RG16). C'est cet ordre qui rend les codes HTTP prévisibles.
- `source=ETUDIANT` : une présence créée par le formateur (EF8) passerait par un autre endpoint et porterait `source=FORMATEUR` (Q14).
