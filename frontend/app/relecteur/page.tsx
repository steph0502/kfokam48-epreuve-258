"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api/endpoints";
import { ApiError } from "@/lib/api/ApiError";
import type { Etudiant, RelectureAffectation } from "@/lib/api/types";
import { Topbar } from "../_components/Topbar";

const PROMOTION_DEMO = 1;
function erreurLisible(e: unknown): string {
  return e instanceof ApiError ? `${e.code} — ${e.message}` : "Erreur inattendue.";
}

export default function RelecteurPage() {
  const [etudiants, setEtudiants] = useState<Etudiant[]>([]);
  const [etudiantId, setEtudiantId] = useState<number | null>(null);
  const [affectations, setAffectations] = useState<RelectureAffectation[]>([]);
  const [historique, setHistorique] = useState<RelectureAffectation[]>([]);
  const [notes, setNotes] = useState<Record<number, string>>({});
  const [commentaires, setCommentaires] = useState<Record<number, string>>({});
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    void api.etudiants.lister(PROMOTION_DEMO).then(setEtudiants).catch((e: unknown) => setErreur(erreurLisible(e)));
  }, []);

  const charger = useCallback(async (id: number) => {
    try {
      const [enAttente, rendues] = await Promise.all([
        api.relectures.lister(id, false),
        api.relectures.lister(id, true),
      ]);
      setAffectations(enAttente);
      setHistorique(rendues);
      setErreur(null);
    } catch (e) { setErreur(erreurLisible(e)); }
  }, []);

  useEffect(() => { if (etudiantId !== null) void charger(etudiantId); }, [etudiantId, charger]);

  async function rendre(affectation: RelectureAffectation) {
    if (etudiantId === null) return;
    const note = Number(notes[affectation.id]);
    const commentaire = commentaires[affectation.id]?.trim() ?? "";
    if (!Number.isInteger(note) || note < 0 || note > 20 || !commentaire) {
      setErreur("Saisissez une note entière entre 0 et 20 et un commentaire.");
      return;
    }
    setChargement(true); setErreur(null); setMessage(null);
    try {
      await api.relectures.rendre(affectation.id, etudiantId, note, commentaire);
      setMessage("Évaluation enregistrée. La note de l’auteur est provisoire en attendant le second avis.");
      await charger(etudiantId);
    } catch (e) { setErreur(erreurLisible(e)); }
    finally { setChargement(false); }
  }

  return (
    <>
      <Topbar active="relecteur" />
      <main className="container">
        <div className="page-head"><div><h1>Espace relecteur</h1><p>Évaluez les exercices attribués par vos pairs.</p></div></div>
        {erreur && <div className="banner banner-error" role="alert">{erreur}</div>}
        {message && <div className="banner banner-success" role="status">{message}</div>}
        <section className="panel">
          <div className="panel-head"><h2>Choisir votre nom</h2></div>
          <div className="panel-body form-group" style={{ maxWidth: 340 }}>
            <label className="form-label" htmlFor="relecteur">Votre nom</label>
            <select id="relecteur" className="form-select" value={etudiantId ?? ""} onChange={(e) => setEtudiantId(Number(e.target.value))}>
              <option value="" disabled>— Choisir —</option>
              {etudiants.map((e) => <option key={e.id} value={e.id}>{e.nom}</option>)}
            </select>
          </div>
        </section>
        {etudiantId !== null && <section className="panel">
          <div className="panel-head"><h2>Évaluations à rendre</h2><button className="ghost-btn" onClick={() => void charger(etudiantId)}>Rafraîchir</button></div>
          <div className="panel-body">
            {affectations.length === 0 ? <p className="muted">Aucune relecture en attente.</p> : affectations.map((a) => (
              <article className="review-card" key={a.id}>
                <div className="panel-head"><h3>Exercice de {a.auteurNom}</h3><a href={a.lien} target="_blank" rel="noreferrer">Ouvrir l’exercice ↗</a></div>
                <div className="form-row">
                  <div className="form-group" style={{ maxWidth: 180 }}><label className="form-label" htmlFor={`note-${a.id}`}>Note /20</label><input id={`note-${a.id}`} className="form-input" type="number" min={0} max={20} step={1} value={notes[a.id] ?? ""} onChange={(e) => setNotes({ ...notes, [a.id]: e.target.value })} /></div>
                  <div className="form-group"><label className="form-label" htmlFor={`commentaire-${a.id}`}>Commentaire</label><textarea id={`commentaire-${a.id}`} className="form-input" value={commentaires[a.id] ?? ""} onChange={(e) => setCommentaires({ ...commentaires, [a.id]: e.target.value })} /></div>
                  <button className="action-btn" disabled={chargement} onClick={() => void rendre(a)}>Rendre l’évaluation</button>
                </div>
              </article>
            ))}
          </div>
        </section>}
        {etudiantId !== null && <section className="panel">
          <div className="panel-head"><h2>Historique des évaluations rendues</h2><span className="status neutral">{historique.length}</span></div>
          <div className="panel-body">
            {historique.length === 0 ? <p className="muted">Aucune évaluation rendue pour le moment.</p> : historique.map((a) => (
              <article className="review-card" key={a.id}>
                <div className="panel-head"><h3>Exercice de {a.auteurNom}</h3><span className="status success">Rendue</span></div>
                <a href={a.lien} target="_blank" rel="noreferrer">Consulter l’exercice ↗</a>
              </article>
            ))}
          </div>
        </section>}
      </main>
      <footer className="app-footer">KFOKAM48 — Direction de la formation · Données de démonstration au démarrage</footer>
    </>
  );
}
