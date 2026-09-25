-- V3 — Deux évaluations distinctes par exercice (enveloppe étape 3).
-- Les relectures existantes sont conservées comme premier pair.
ALTER TABLE relecture DROP CONSTRAINT uk_relecture_exercice;

ALTER TABLE relecture
    ADD COLUMN numero_relecteur INTEGER NOT NULL DEFAULT 1;

ALTER TABLE relecture
    ADD CONSTRAINT ck_relecture_numero CHECK (numero_relecteur IN (1, 2));

ALTER TABLE relecture
    ADD CONSTRAINT uk_relecture_exercice_numero UNIQUE (exercice_id, numero_relecteur);

ALTER TABLE relecture
    ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);
