-- V2 — Données de démonstration (référentiel).
-- Le sujet impose des données de démo chargées au démarrage : le correcteur
-- doit pouvoir tester l'application sans la peupler à la main.
-- AUCUN id explicite : les lignes insérées avec un id fixé ne font pas avancer
-- les séquences IDENTITY (ni H2 ni PostgreSQL) et provoqueraient une collision
-- au premier INSERT normal. Les FK sont résolues par sous-requête.
-- Sur une base fraîche, la promotion obtient id=1 et les étudiants 1 à 4.
-- La session de démonstration (code frais) est créée au démarrage par
-- DonneesDemoConfig : son code expire 15 minutes après, il ne peut pas être figé ici.

INSERT INTO promotion (nom) VALUES ('KFOKAM48 — Promotion 2026');

INSERT INTO etudiant (nom, promotion_id)
    SELECT e.nom, p.id
    FROM promotion p,
         (VALUES ('Alice Nkuli'), ('Bruno Mbarga'), ('Chantal Owona'), ('Davy Tchoumi')) AS e(nom)
    WHERE p.nom = 'KFOKAM48 — Promotion 2026';
