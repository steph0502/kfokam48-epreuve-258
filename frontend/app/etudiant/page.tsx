"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/ApiError";
import type { Etudiant, ExerciceCree, ExerciceEtudiant, Presence } from "@/lib/api/types";
import { Topbar } from "../_components/Topbar";

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
  const [mesExercices, setMesExercices] = useState<ExerciceEtudiant[]>([]);

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

  const chargerExercices = useCallback(async (id: number) => {
    try {
      setMesExercices(await api.etudiants.exercices(id));
    } catch (e) {
      setErreur(erreurLisible(e));
    }
  }, []);

  useEffect(() => {
    if (etudiantId !== null) void chargerExercices(etudiantId);
  }, [etudiantId, chargerExercices]);

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
      await chargerExercices(presence.etudiantId);
      await chargerEtudiants();
    } catch (e) {
      setErreur(erreurLisible(e));
    } finally {
      setChargement(false);
    }
  }

  return (
    <>
      <Topbar active="etudiant" />
      <main className="container">
        <div className="page-head">
          <div>
            <h1>Espace étudiant</h1>
            <p>Choisissez votre nom, pointez avec le code, déposez votre exercice.</p>
          </div>
          <Link href="/formateur" className="ghost-btn">
            ← Espace formateur
          </Link>
        </div>

        {erreur && (
          <div className="banner banner-error" role="alert">
            {erreur}
          </div>
        )}

        <section className="panel">
          <div className="step">
            <div className="step-title">
              <span className={`step-badge${etudiantId !== null ? " done" : ""}`}>1</span>
              <h2>Choisir son nom</h2>
            </div>
            {etudiants.length === 0 ? (
              <p className="muted">Chargement de la liste…</p>
            ) : (
              <div className="form-group" style={{ maxWidth: 340 }}>
                <label className="form-label" htmlFor="etudiant">
                  Votre nom
                </label>
                <select
                  id="etudiant"
                  className="form-select"
                  value={etudiantId ?? ""}
                  onChange={(e) => setEtudiantId(Number(e.target.value))}
                >
                  <option value="" disabled>
                    — Choisir —
                  </option>
                  {etudiants.map((e) => (
                    <option key={e.id} value={e.id}>
                      {e.nom}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>
        </section>

        {etudiantId !== null && !presence && (
          <section className="panel">
            <div className="step">
              <div className="step-title">
                <span className="step-badge">2</span>
                <h2>Marquer sa présence</h2>
              </div>
              <div className="form-row">
                <div className="form-group" style={{ maxWidth: 260 }}>
                  <label className="form-label" htmlFor="code">
                    Code du formateur
                  </label>
                  <input
                    id="code"
                    className="form-input code-input"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                    maxLength={6}
                    placeholder="ABC123"
                  />
                </div>
                <button
                  className="action-btn"
                  onClick={marquerPresence}
                  disabled={chargement || code.trim() === ""}
                >
                  Marquer ma présence
                </button>
              </div>
            </div>
          </section>
        )}

        {presence && (
          <section className="panel">
            <div className="step">
              <div className="step-title">
                <span className="step-badge done">2</span>
                <h2>Présence enregistrée</h2>
                <span className="status success">✓ Validée</span>
              </div>
              <p className="muted">Votre présence est enregistrée (source : {presence.source}).</p>
            </div>
          </section>
        )}

        {presence && !exercice && (
          <section className="panel">
            <div className="step">
              <div className="step-title">
                <span className="step-badge">3</span>
                <h2>Déposer le lien de son exercice</h2>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="lien">
                    Lien de l&apos;exercice (http/https)
                  </label>
                  <input
                    id="lien"
                    className="form-input"
                    value={lien}
                    onChange={(e) => setLien(e.target.value)}
                    placeholder="https://…"
                  />
                </div>
                <button
                  className="action-btn"
                  onClick={deposerExercice}
                  disabled={chargement || lien.trim() === ""}
                >
                  Déposer
                </button>
              </div>
            </div>
          </section>
        )}

        {exercice && (
          <section className="panel">
            <div className="step">
              <div className="step-title">
                <span className="step-badge done">3</span>
                <h2>Exercice déposé</h2>
                <span
                  className={`status ${exercice.statut === "EN_ATTENTE" ? "warning" : "info"}`}
                >
                  {exercice.statut}
                </span>
              </div>
              <p className="muted">
                {exercice.statut === "EN_ATTENTE"
                  ? "Des relecteurs seront affectés au fur et à mesure des présences des autres étudiants."
                  : "Au moins un pair a été affecté. Vous verrez ici les avis et la note retenue dès qu’ils seront rendus."}
              </p>
            </div>
          </section>
        )}

        {etudiantId !== null && (
          <section className="panel">
            <div className="panel-head">
              <h2>Mes exercices et retours</h2>
              <button className="ghost-btn" onClick={() => void chargerExercices(etudiantId)}>Rafraîchir</button>
            </div>
            <div className="panel-body">
              {mesExercices.length === 0 ? (
                <p className="muted">Aucun exercice déposé pour le moment.</p>
              ) : mesExercices.map((item) => (
                <article className="panel" key={item.id}>
                  <div className="panel-head">
                    <h3>Exercice #{item.id}</h3>
                    <span className={`status ${item.noteProvisoire ? "warning" : item.noteRetenue === null ? "neutral" : "success"}`}>
                      {item.noteRetenue === null ? item.statut : `${item.noteRetenue.toFixed(1)}/20${item.noteProvisoire ? " · provisoire" : " · définitive"}`}
                    </span>
                  </div>
                  <div className="panel-body">
                    <a href={item.lien} target="_blank" rel="noreferrer">Consulter l’exercice</a>
                    {item.evaluations.length === 0 ? <p className="muted">Aucune évaluation rendue pour le moment.</p> : item.evaluations.map((evaluation, index) => (
                      <div className="review-feedback" key={`${item.id}-${index}`}>
                        <strong>Évaluation {index + 1} · {evaluation.note}/20</strong>
                        <p>{evaluation.commentaire}</p>
                      </div>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          </section>
        )}
      </main>
      <footer className="app-footer">
        KFOKAM48 — Direction de la formation · Données de démonstration au démarrage
      </footer>
    </>
  );
}
