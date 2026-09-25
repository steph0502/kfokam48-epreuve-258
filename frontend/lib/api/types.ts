/**
 * Types alignés sur api/contrat.yaml — une seule source de vérité côté front.
 * Aucune règle métier ici (F3) : la moyenne vient de l'API, jamais recalculée.
 */
export interface SessionCreee {
  id: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
}

export interface Presence {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: "ETUDIANT" | "FORMATEUR";
}

export interface LigneTableau {
  etudiantId: number;
  nom: string;
  presences: number;
  exercicesDeposes: number;
  /** null si aucune note reçue (RG15) — calculée côté API, jamais recalculée (F3). */
  moyenne: number | null;
  moyenneProvisoire: boolean;
  relecturesEnAttente: number;
}

export interface Etudiant {
  id: number;
  nom: string;
}

export interface ExerciceCree {
  id: number;
  statut: string;
}

export interface EvaluationExercice {
  note: number;
  commentaire: string;
}

export interface ExerciceEtudiant {
  id: number;
  sessionId: number;
  lien: string;
  statut: string;
  noteRetenue: number | null;
  noteProvisoire: boolean;
  evaluations: EvaluationExercice[];
}

export interface RelectureAffectation {
  id: number;
  exerciceId: number;
  auteurNom: string;
  lien: string;
  rendue: boolean;
}
