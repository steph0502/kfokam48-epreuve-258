package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.codegenerator.CodeGenerator;
import com.kfokam48.domain.SessionJpa;
import com.kfokam48.repository.PromotionRepository;
import com.kfokam48.repository.SessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Ouverture d'une session (EF1) : le formateur reçoit un code de présence
 * unique (RG18) qui expire 15 minutes après l'ouverture (RG1).
 */
@Service
public class SessionService {

    static final Duration DUREE_CODE = Duration.ofMinutes(15); // RG1

    private final SessionRepository sessions;
    private final PromotionRepository promotions;
    private final CodeGenerator codeGenerator;
    private final Clock clock;

    public SessionService(SessionRepository sessions, PromotionRepository promotions,
                          CodeGenerator codeGenerator, Clock clock) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.codeGenerator = codeGenerator;
        this.clock = clock;
    }

    @Transactional
    public SessionJpa ouvrir(String titre, Long promotionId) {
        promotions.findById(promotionId).orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST, "PROMOTION_INCONNUE",
                "La promotion " + promotionId + " n'existe pas."));
        Instant maintenant = Instant.now(clock);
        SessionJpa session = new SessionJpa(
                titre, promotionId, genererCodeUnique(), maintenant, maintenant.plus(DUREE_CODE));
        return sessions.save(session);
    }

    /** RG18 : le code doit être unique — régénération en cas de collision. */
    private String genererCodeUnique() {
        for (int i = 0; i < 20; i++) {
            String candidat = codeGenerator.nouveauCode();
            if (!sessions.existsByCode(candidat)) {
                return candidat;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Impossible de générer un code de session unique (RG18).");
    }
}
