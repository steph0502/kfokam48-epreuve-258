# D1 — Diagramme de cas d'utilisation

> Le relecteur n'est pas un acteur distinct : c'est un étudiant dans un état
> d'assignation (voir cahier des charges §2). Il apparaît comme spécialisation.

```mermaid
graph LR
    FORMATEUR((Formateur))
    ETUDIANT((Étudiant))
    RELECTEUR(("Relecteur<br/>(étudiant assigné)"))

    subgraph Système["Système KFOKAM48"]
        UC1(["Ouvrir une session<br/>et obtenir le code (EF1)"])
        UC2(["Clôturer une session (EF7)"])
        UC3(["Ajouter une présence<br/>à la main (EF8)"])
        UC4(["Consulter le tableau<br/>récapitulatif (EF6)"])
        UC5(["Marquer sa présence<br/>avec le code (EF2)"])
        UC6(["Déposer le lien de<br/>son exercice (EF3)"])
        UC7(["Remplacer son lien (EF9)"])
        UC8(["Consulter sa note et<br/>son commentaire (EF10)"])
        UC9(["Choisir son nom dans<br/>la liste (EF11)"])
        UC10(["Rendre une relecture :<br/>deux avis, note provisoire puis moyenne (EF5)"])
    end

    FORMATEUR --> UC1
    FORMATEUR --> UC2
    FORMATEUR --> UC3
    FORMATEUR --> UC4

    ETUDIANT --> UC9
    ETUDIANT --> UC5
    ETUDIANT --> UC6
    ETUDIANT --> UC7
    ETUDIANT --> UC8

    RELECTEUR --> UC10
    RELECTEUR -.->|hérite des cas| ETUDIANT
```

**Contrôles associés (cas d'erreur du contrat) :**

- UC5 : `400 CODE_INCONNU` · `409 DEJA_PRESENT` (RG16) · `410 CODE_EXPIRE` (RG1) · refus après clôture (RG2)
- UC6 : `400 LIEN_INVALIDE` · `409 EXERCICE_DEJA_DEPOSE` (RG17) · refus après clôture (RG10)
- UC10 : `400 NOTE_INVALIDE` (RG7) · `403 AUTO_RELECTURE` (RG4) · `409 RELECTURE_DEJA_RENDUE` (RG8)
- UC4 : `404 PROMOTION_INCONNUE`
