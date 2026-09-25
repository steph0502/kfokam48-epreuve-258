"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/ApiError";
import type { LigneTableau, SessionCreee } from "@/lib/api/types";
import { Topbar } from "../_components/Topbar";

const PROMOTION_DEMO = 1;

function erreurLisible(e: unknown): string {
  if (e instanceof ApiError) {
    return `${e.code} — ${e.message}`;
  }
  return "Erreur inattendue.";
}

export default function FormateurPage() {
  const [session, setSession] = useState<SessionCreee | null>(null);
  const [cloturee, setCloturee] = useState(false);
  const [tableau, setTableau] = useState<LigneTableau[]>([]);
  const [titre, setTitre] = useState("Cours du matin");
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  const rafraichirTableau = useCallback(async () => {
    try {
      setTableau(await api.tableau.lire(PROMOTION_DEMO));
      setErreur(null);
    } catch (e) {
      setErreur(erreurLisible(e));
    }
  }, []);

  useEffect(() => {
    void rafraichirTableau();
  }, [rafraichirTableau]);

  async function ouvrirSession() {
    setChargement(true);
    setErreur(null);
    try {
      const s = await api.sessions.ouvrir(titre, PROMOTION_DEMO);
      setSession(s);
      setCloturee(false);
      await rafraichirTableau();
    } catch (e) {
      setErreur(erreurLisible(e));
    } finally {
      setChargement(false);
    }
  }

  async function cloturerSession() {
    if (!session) return;
    setChargement(true);
    setErreur(null);
    try {
      await api.sessions.cloturer(session.id);
      setCloturee(true);
    } catch (e) {
      setErreur(erreurLisible(e));
    } finally {
      setChargement(false);
    }
  }

  return (
    <>
      <Topbar active="formateur" />
      <main className="container">
        <div className="page-head">
          <div>
            <h1>Espace formateur</h1>
            <p>Ouvrez une session, projetez le code, suivez la promotion en direct.</p>
          </div>
          <Link href="/etudiant" className="ghost-btn">
            Espace étudiant →
          </Link>
        </div>

        {erreur && (
          <div className="banner banner-error" role="alert">
            {erreur}
          </div>
        )}

        <section className="panel">
          <div className="panel-head">
            <h2>Ouvrir une session</h2>
          </div>
          <div className="panel-body">
            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="titre">
                  Titre du cours
                </label>
                <input
                  id="titre"
                  className="form-input"
                  value={titre}
                  onChange={(e) => setTitre(e.target.value)}
                />
              </div>
              <button
                className="action-btn"
                onClick={ouvrirSession}
                disabled={chargement || titre.trim() === ""}
              >
                Ouvrir et obtenir le code
              </button>
            </div>
          </div>
        </section>

        {session && (
          <section className="panel">
            <div className="panel-head">
              <h2>{cloturee ? "Session clôturée" : "Session ouverte"}</h2>
              <span className={`status ${cloturee ? "neutral" : "success"}`}>
                {cloturee ? "Clôturée" : "Ouverte"}
              </span>
            </div>
            <div className="panel-body">
              <span className="code-label">Code de présence à projeter</span>
              <span className="code-display">{session.code}</span>
              <p className="muted">
                Valable jusqu&apos;à {new Date(session.expirationAt).toLocaleTimeString("fr-FR")}.
              </p>
              <div>
                <button className="ghost-btn" onClick={cloturerSession} disabled={chargement || cloturee}>
                  Clôturer la session
                </button>
              </div>
            </div>
          </section>
        )}

        <section className="panel">
          <div className="panel-head">
            <h2>Tableau récapitulatif</h2>
            <button className="ghost-btn" onClick={() => void rafraichirTableau()}>
              Rafraîchir
            </button>
          </div>
          <div className="table-wrap">
            <table className="ui-table">
              <thead>
                <tr>
                  <th>Étudiant</th>
                  <th>Présences</th>
                  <th>Exercices déposés</th>
                  <th>Moyenne /20</th>
                  <th>Relectures en attente</th>
                </tr>
              </thead>
              <tbody>
                {tableau.length === 0 && (
                  <tr>
                    <td className="empty-row" colSpan={5}>
                      Aucune donnée pour l&apos;instant — ouvrez une session et attendez les
                      premières présences.
                    </td>
                  </tr>
                )}
                {tableau.map((ligne) => (
                  <tr key={ligne.etudiantId}>
                    <td className="strong">{ligne.nom}</td>
                    <td>{ligne.presences}</td>
                    <td>{ligne.exercicesDeposes}</td>
                    {/* La moyenne vient de l'API (RG15) — jamais recalculée ici (F3). */}
                    <td>
                      {ligne.moyenne === null ? (
                        <span className="muted">—</span>
                      ) : (
                        <span className={`status ${ligne.moyenneProvisoire ? "warning" : ligne.moyenne >= 10 ? "success" : "danger"}`}>
                          {ligne.moyenne.toFixed(1)}{ligne.moyenneProvisoire ? " · provisoire" : ""}
                        </span>
                      )}
                    </td>
                    <td>
                      {ligne.relecturesEnAttente > 0 ? (
                        <span className="status warning">{ligne.relecturesEnAttente}</span>
                      ) : (
                        <span className="muted">0</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      </main>
      <footer className="app-footer">
        KFOKAM48 — Direction de la formation · Données de démonstration au démarrage
      </footer>
    </>
  );
}
