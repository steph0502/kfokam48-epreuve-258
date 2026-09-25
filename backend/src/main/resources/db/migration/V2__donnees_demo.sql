-- V2 — Données de démonstration (référentiel).
-- Le sujet impose des données de démo chargées au démarrage : le correcteur
-- doit pouvoir tester l'application sans la peupler à la main.
-- La session de démonstration (code frais) est créée au démarrage par
-- DonneesDemoConfig : son code expire 15 minutes après, il ne peut pas être figé ici.

INSERT INTO promotion (id, nom) VALUES (1, 'KFOKAM48 — Promotion 2026');

INSERT INTO etudiant (id, nom, promotion_id) VALUES
    (1, 'Alice Nkuli', 1),
    (2, 'Bruno Mbarga', 1),
    (3, 'Chantal Owona', 1),
    (4, 'Davy Tchoumi', 1);
