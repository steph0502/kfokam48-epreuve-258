package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.PresenceJpa;
import com.kfokam48.domain.SessionJpa;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.PresenceRepository;
import com.kfokam48.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Marquage de la présence (EF2). Règles appliquées :
 * RG1 — code expiré 15 minutes après l'ouverture (410) ;
 * RG2 — pas de présence après la clôture de la session (Q3) ;
 * RG16 — une seule présence par étudiant et par session (409).
 */
@Service
public class PresenceService {

    private final SessionRepository sessions;
    private final EtudiantRepository etudiants;
    private final PresenceRepository presences;
    private final Clock clock;

    public PresenceService(SessionRepository sessions, EtudiantRepository etudiants,
                           PresenceRepository presences, Clock clock) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.presences = presences;
        this.clock = clock;
    }

    public PresenceJpa marquer(String code, Long etudiantId) {
        if (!etudiants.existsById(etudiantId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ETUDIANT_INCONNU",
                    "L'étudiant " + etudiantId + " n'existe pas.");
        }
        SessionJpa session = sessions.findByCode(code).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST, "CODE_INCONNU",
                "Ce code de présence ne correspond à aucune session ouverte."));

        Instant maintenant = Instant.now(clock);
        if (session.getClotureAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SESSION_CLOTUREE",
                    "La session est clôturée : la présence n'est plus possible (RG2).");
        }
        if (maintenant.isAfter(session.getExpirationAt())) {
            throw new ApiException(HttpStatus.GONE, "CODE_EXPIRE",
                    "Le code de présence a expiré (RG1).");
        }
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiantId)) {
            throw new ApiException(HttpStatus.CONFLICT, "DEJA_PRESENT",
                    "Cet étudiant a déjà marqué sa présence pour cette session (RG16).");
        }
        return presences.save(new PresenceJpa(session.getId(), etudiantId, PresenceJpa.Source.ETUDIANT, maintenant));
    }
}
