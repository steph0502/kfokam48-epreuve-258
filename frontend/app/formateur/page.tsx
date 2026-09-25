"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/ApiError";
import type { LigneTableau, SessionCreee } from "@/lib/api/types";

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
    <main>
      <h1>Espace formateur</h1>
      <p>
        <Link href="/etudiant">Aller à l&apos;espace étudiant →</Link>
      </p>

      <section>
        <h2>Ouvrir une session</h2>
        <label>
          Titre du cours :{" "}
          <input value={titre} onChange={(e) => setTitre(e.target.value)} />
        </label>{" "}
        <button onClick={ouvrirSession} disabled={chargement || titre.trim() === ""}>
          Ouvrir et obtenir le code
        </button>
      </section>

      {session && (
        <section>
          <h2>{cloturee ? "Session clôturée" : "Session ouverte"}</h2>
          <p>
            Code de présence à projeter :{" "}
            <strong style={{ fontSize: "2rem", letterSpacing: "0.3em" }}>
              {session.code}
            </strong>
          </p>
          <p>Valable jusqu&apos;à {new Date(session.expirationAt).toLocaleTimeString("fr-FR")}.</p>
          <button onClick={cloturerSession} disabled={chargement || cloturee}>
            Clôturer la session
          </button>
        </section>
      )}

      {erreur && (
        <p role="alert" style={{ color: "crimson" }}>
          {erreur}
        </p>
      )}

      <section>
        <h2>Tableau récapitulatif</h2>
        <button onClick={() => void rafraichirTableau()}>Rafraîchir</button>
        <table border={1} cellPadding={6}>
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
            {tableau.map((ligne) => (
              <tr key={ligne.etudiantId}>
                <td>{ligne.nom}</td>
                <td>{ligne.presences}</td>
                <td>{ligne.exercicesDeposes}</td>
                {/* La moyenne vient de l'API (RG15) — jamais recalculée ici (F3). */}
                <td>{ligne.moyenne === null ? "—" : ligne.moyenne.toFixed(1)}</td>
                <td>{ligne.relecturesEnAttente}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  );
}
