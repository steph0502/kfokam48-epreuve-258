package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.ExerciceJpa;
import com.kfokam48.domain.PresenceJpa;
import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.domain.SessionJpa;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.ExerciceRepository;
import com.kfokam48.repository.PresenceRepository;
import com.kfokam48.repository.RelectureRepository;
import com.kfokam48.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Dépôt d'exercice (EF3) et assignation du relecteur (EF4).
 * RG17 — un seul exercice par étudiant et par session (409) ;
 * RG10/Q12 — dépôt interdit après la clôture ;
 * RG4 — le relecteur n'est jamais l'auteur (pas d'auto-relecture) ;
 * RG6/RG7 — un seul relecteur, choisi au hasard parmi les présents ;
 * RG14 — si personne d'autre n'est présent, l'exercice reste EN_ATTENTE
 * et l'assignation est retentée à chaque nouvelle présence.
 */
@Service
public class ExerciceService {

    private static final SecureRandom HASARD = new SecureRandom();

    private final SessionRepository sessions;
    private final EtudiantRepository etudiants;
    private final ExerciceRepository exercices;
    private final PresenceRepository presences;
    private final RelectureRepository relectures;
    private final Clock clock;

    public ExerciceService(SessionRepository sessions, EtudiantRepository etudiants,
                           ExerciceRepository exercices, PresenceRepository presences,
                           RelectureRepository relectures, Clock clock) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.exercices = exercices;
        this.presences = presences;
        this.relectures = relectures;
        this.clock = clock;
    }

    @Transactional
    public ExerciceJpa deposer(Long sessionId, Long etudiantId, String lien) {
        SessionJpa session = sessions.findById(sessionId).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST, "SESSION_INCONNUE",
                "La session " + sessionId + " n'existe pas."));
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SESSION_CLOTUREE",
                    "La session est clôturée : le dépôt n'est plus possible (RG10).");
        }
        if (!etudiants.existsById(etudiantId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ETUDIANT_INCONNU",
                    "L'étudiant " + etudiantId + " n'existe pas.");
        }
        validerLien(lien);
        if (exercices.existsBySessionIdAndEtudiantId(sessionId, etudiantId)) {
            throw new ApiException(HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE",
                    "Un exercice a déjà été déposé pour cette session (RG17).");
        }
        ExerciceJpa exercice = exercices.save(new ExerciceJpa(sessionId, etudiantId, lien, Instant.now(clock)));
        assignerSiPossible(exercice); // EF4 : tentative immédiate après le dépôt
        return exercice;
    }

    /**
     * RG14 : retente l'assignation des exercices EN_ATTENTE de la session.
     * Appelé après chaque nouvelle présence enregistrée.
     */
    @Transactional
    public void retenterAssignations(Long sessionId) {
        for (ExerciceJpa exercice : exercices.findBySessionIdAndStatut(sessionId, ExerciceJpa.Statut.EN_ATTENTE.name())) {
            assignerSiPossible(exercice);
        }
    }

    private void assignerSiPossible(ExerciceJpa exercice) {
        List<Long> candidats = presences.findBySessionId(exercice.getSessionId()).stream()
                .map(PresenceJpa::getEtudiantId)
                .filter(id -> !id.equals(exercice.getEtudiantId())) // RG4 : l'auteur est exclu
                .toList();
        if (candidats.isEmpty()) {
            return; // RG14 : aucun autre présent — l'exercice reste EN_ATTENTE
        }
        Long relecteurId = candidats.get(HASARD.nextInt(candidats.size())); // RG6, RG7
        relectures.save(new RelectureJpa(exercice.getId(), relecteurId));
        exercice.assigner(); // EN_ATTENTE → ASSIGNE (D4)
    }

    private void validerLien(String lien) {
        if (lien == null || lien.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LIEN_INVALIDE",
                    "Le lien de l'exercice est obligatoire.");
        }
        try {
            URI uri = URI.create(lien.trim());
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "LIEN_INVALIDE",
                        "Le lien doit être une URL http(s) valide.");
            }
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LIEN_INVALIDE",
                    "Le lien doit être une URL valide.");
        }
    }
}
