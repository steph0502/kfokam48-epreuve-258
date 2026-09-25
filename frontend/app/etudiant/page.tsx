"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/ApiError";
import type { Etudiant, ExerciceCree, Presence } from "@/lib/api/types";

const PROMOTION_DEMO = 1;

function erreurLisible(e: unknown): string {
  if (e instanceof ApiError) {
    return `${e.code} — ${e.message}`;
  }
  return "Erreur inattendue.";
}

export default function EtudiantPage() {
  const [etudiants, setEtudiants] = useState<Etudiant[]>([]);
  const [etudiantId, setEtudiantId] = useState<number | null>(null);

  const [code, setCode] = useState("");
  const [presence, setPresence] = useState<Presence | null>(null);

  const [lien, setLien] = useState("");
  const [exercice, setExercice] = useState<ExerciceCree | null>(null);

  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const chargerEtudiants = useCallback(async () => {
    try {
      setEtudiants(await api.etudiants.lister(PROMOTION_DEMO));
      setErreur(null);
    } catch (e) {
      setErreur(erreurLisible(e));
    }
  }, []);

  useEffect(() => {
    void chargerEtudiants();
  }, [chargerEtudiants]);

  async function marquerPresence() {
    if (etudiantId === null || code.trim() === "") return;
    setChargement(true);
    setErreur(null);
    try {
      const p = await api.presences.marquer(code.trim().toUpperCase(), etudiantId);
      setPresence(p);
      await chargerEtudiants();
    } catch (e) {
      setErreur(erreurLisible(e));
    } finally {
      setChargement(false);
    }
  }

  async function deposerExercice() {
    if (presence === null || lien.trim() === "") return;
    setChargement(true);
    setErreur(null);
    try {
      // sessionId reçu de la présence : le dépôt cible forcément la bonne session (EF3).
      const exercice = await api.exercices.deposer(presence.sessionId, presence.etudiantId, lien.trim());
      setExercice(exercice);
      await chargerEtudiants();
    } catch (e) {
      setErreur(erreurLisible(e));
    } finally {
      setChargement(false);
    }
  }

  return (
    <main>
      <h1>Espace étudiant</h1>
      <p>
        <Link href="/formateur">← Espace formateur</Link>
      </p>

      <section>
        <h2>1. Choisir son nom</h2>
        {etudiants.length === 0 ? (
          <p>Chargement de la liste…</p>
        ) : (
          <select value={etudiantId ?? ""} onChange={(e) => setEtudiantId(Number(e.target.value))}>
            <option value="" disabled>
              — Choisir —
            </option>
            {etudiants.map((e) => (
              <option key={e.id} value={e.id}>
                {e.nom}
              </option>
            ))}
          </select>
        )}
      </section>

      {etudiantId !== null && !presence && (
        <section>
          <h2>2. Marquer sa présence</h2>
          <label>
            Code du formateur :{" "}
            <input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              maxLength={6}
              placeholder="6 caractères"
            />
          </label>{" "}
          <button onClick={marquerPresence} disabled={chargement || code.trim() === ""}>
            Marquer ma présence
          </button>
        </section>
      )}

      {presence && (
        <section>
          <h2>2. Présence enregistrée ✓</h2>
          <p>Votre présence est enregistrée (source : {presence.source}).</p>
        </section>
      )}

      {presence && !exercice && (
        <section>
          <h2>3. Déposer le lien de son exercice</h2>
          <label>
            Lien (http/https) :{" "}
            <input value={lien} onChange={(e) => setLien(e.target.value)} placeholder="https://…" />
          </label>{" "}
          <button onClick={deposerExercice} disabled={chargement || lien.trim() === ""}>
            Déposer
          </button>
        </section>
      )}

      {exercice && (
        <section>
          <h2>3. Exercice déposé ✓</h2>
          <p>
            Statut : <strong>{exercice.statut}</strong>
            {exercice.statut === "EN_ATTENTE"
              ? " — un relecteur sera assigné dès qu'un autre étudiant sera présent."
              : " — un relecteur a été assigné."}
          </p>
        </section>
      )}

      {erreur && (
        <p role="alert" style={{ color: "crimson" }}>
          {erreur}
        </p>
      )}
    </main>
  );
}
