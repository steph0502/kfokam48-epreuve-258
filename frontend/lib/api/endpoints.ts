import { requete } from "./client";
import type { Etudiant, ExerciceCree, ExerciceEtudiant, LigneTableau, Presence, RelectureAffectation, SessionCreee } from "./types";

/**
 * Endpoints typés de l'API (F3) : chaque écran consomme ces fonctions,
 * jamais fetch directement. Les règles métier restent côté backend.
 */
export const api = {
  sessions: {
    /** EF1 — ouvrir une session et obtenir le code de présence. */
    ouvrir: (titre: string, promotionId: number) =>
      requete<SessionCreee>("/api/sessions", { method: "POST", corps: { titre, promotionId } }),

    /** EF7 — clôturer la session. */
    cloturer: (id: number) =>
      requete<{ id: number; clotureAt: string }>(`/api/sessions/${id}/cloture`, { method: "POST" }),
  },

  presences: {
    /** EF2 — marquer sa présence avec le code. */
    marquer: (code: string, etudiantId: number) =>
      requete<Presence>("/api/presences", { method: "POST", corps: { code, etudiantId } }),
  },

  exercices: {
    /** EF3 — déposer le lien de son exercice. */
    deposer: (sessionId: number, etudiantId: number, lien: string) =>
      requete<ExerciceCree>("/api/exercices", { method: "POST", corps: { sessionId, etudiantId, lien } }),
  },

  tableau: {
    /** EF6 — tableau récapitulatif du formateur (la moyenne vient de l'API). */
    lire: (promotionId: number) =>
      requete<LigneTableau[]>("/api/tableau", { parametres: { promotionId: String(promotionId) } }),
  },

  etudiants: {
    /** EF11 — liste des étudiants pour choisir son nom (Q1). */
    lister: (promotionId: number) =>
      requete<Etudiant[]>("/api/etudiants", { parametres: { promotionId: String(promotionId) } }),
    exercices: (id: number) => requete<ExerciceEtudiant[]>(`/api/etudiants/${id}/exercices`),
  },

  relectures: {
    lister: (etudiantId: number, rendue = false) =>
      requete<RelectureAffectation[]>("/api/relectures", {
        parametres: { etudiantId: String(etudiantId), rendue: String(rendue) },
      }),
    rendre: (id: number, etudiantId: number, note: number, commentaire: string) =>
      requete<void>(`/api/relectures/${id}?etudiantId=${etudiantId}`, {
        method: "POST", corps: { note, commentaire },
      }),
  },
};
