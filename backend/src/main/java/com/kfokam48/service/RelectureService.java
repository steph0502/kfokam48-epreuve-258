package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.ExerciceJpa;
import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.domain.SessionJpa;
import com.kfokam48.repository.ExerciceRepository;
import com.kfokam48.repository.RelectureRepository;
import com.kfokam48.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Rendu d'une relecture (EF5) et correction (Q10, H9 — décision provisoire).
 *
 * Identité du relecteur (H9, tranchée) : le POST imposé ne porte pas d'identité
 * dans son corps, elle transite par le paramètre de requête etudiantId, à l'image
 * de GET /api/relectures?etudiantId=. Le corps {note, commentaire} reste
 * strictement conforme au contrat.
 *
 * RG7 — note entière 0–20 (400 NOTE_INVALIDE) ;
 * RG4 — jamais d'auto-relecture (403 AUTO_RELECTURE) ;
 * RG8 — le POST est la création initiale : une seconde soumission renvoie
 *       409 RELECTURE_DEJA_RENDUE ; la correction passe par PUT, possible
 *       tant que la session n'est pas clôturée (Q10), définitive après (Q15).
 * H10 — la soumission initiale reste possible après la clôture : l'analyse n'interdit
 *       que la correction après clôture (audit EF5), Q12 tolère les dépôts tardifs et
 *       Q11 exige la visibilité des relectures en attente — la relecture d'un exercice
 *       assigné doit pouvoir aboutir, sinon l'étudiant relu perd sa note.
 */
@Service
public class RelectureService {

    private final RelectureRepository relectures;
    private final ExerciceRepository exercices;
    private final SessionRepository sessions;
    private final Clock clock;

    public RelectureService(RelectureRepository relectures, ExerciceRepository exercices,
                            SessionRepository sessions, Clock clock) {
        this.relectures = relectures;
        this.exercices = exercices;
        this.sessions = sessions;
        this.clock = clock;
    }

    /** Création initiale — POST /api/relectures/{id} du contrat. */
    @Transactional
    public RelectureJpa rendre(Long relectureId, Long etudiantId, Integer note, String commentaire) {
        RelectureJpa relecture = existante(relectureId);
        validerNote(note);                                        // RG7
        verifierIdentite(relecture, etudiantId);                  // RG4 + relecteur assigné
        if (relecture.estRendue()) {
            throw new ApiException(HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE",
                    "Cette relecture a déjà été rendue ; passez par la correction tant que la session est ouverte (RG8).");
        }
        relecture.rendre(note, commentaire.trim(), Instant.now(clock));
        marquerExerciceReluSiComplet(relecture);                 // RELU après les deux rendus
        return relecture;
    }

    /** Correction avant clôture — PUT /api/relectures/{id} (Q10, H9, décision provisoire). */
    @Transactional
    public RelectureJpa corriger(Long relectureId, Long etudiantId, Integer note, String commentaire) {
        RelectureJpa relecture = existante(relectureId);
        SessionJpa session = sessionDe(relecture);
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "CORRECTION_INTERDITE",
                    "La session est clôturée : la relecture rendue est définitive (Q15, RG8).");
        }
        validerNote(note);                                        // RG7
        verifierIdentite(relecture, etudiantId);                  // RG4 + relecteur assigné
        if (!relecture.estRendue()) {
            throw new ApiException(HttpStatus.CONFLICT, "RELECTURE_NON_RENDUE",
                    "Cette relecture n'a pas encore été rendue : utilisez la soumission initiale.");
        }
        relecture.corriger(note, commentaire.trim());
        return relecture;
    }

    private RelectureJpa existante(Long relectureId) {
        return relectures.findById(relectureId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "RELECTURE_INCONNUE",
                "La relecture " + relectureId + " n'existe pas."));
    }

    private SessionJpa sessionDe(RelectureJpa relecture) {
        ExerciceJpa exercice = exercices.findById(relecture.getExerciceId()).orElseThrow(() -> new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE", "Exercice de la relecture introuvable."));
        return sessions.findById(exercice.getSessionId()).orElseThrow(() -> new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE", "Session de la relecture introuvable."));
    }

    private void validerNote(Integer note) {
        if (note == null || note < 0 || note > 20) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NOTE_INVALIDE",
                    "La note doit être un entier entre 0 et 20 (RG7).");
        }
    }

    /** RG4 d'abord (contrat) : l'auteur ne peut jamais relecter, même sans être le relecteur assigné. */
    private void verifierIdentite(RelectureJpa relecture, Long etudiantId) {
        ExerciceJpa exercice = exercices.findById(relecture.getExerciceId()).orElseThrow();
        if (exercice.getEtudiantId().equals(etudiantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "AUTO_RELECTURE",
                    "Un étudiant ne peut pas relire son propre exercice (RG4).");
        }
        if (!relecture.getRelecteurId().equals(etudiantId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "RELECTEUR_NON_ASSIGNE",
                    "Seul le relecteur assigné peut intervenir sur cette relecture (RG6, RG7).");
        }
    }

    private void marquerExerciceReluSiComplet(RelectureJpa relecture) {
        List<RelectureJpa> affectations = relectures
                .findByExerciceIdOrderByNumeroRelecteurAsc(relecture.getExerciceId());
        if (affectations.size() == 2 && affectations.stream().allMatch(RelectureJpa::estRendue)) {
            exercices.findById(relecture.getExerciceId()).ifPresent(ExerciceJpa::marquerRelu);
        }
    }
}
