package com.kfokam48.web;

import com.kfokam48.domain.PresenceJpa;
import com.kfokam48.service.PresenceService;
import com.kfokam48.web.dto.MarquerPresenceRequest;
import com.kfokam48.web.dto.PresenceDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EF2 — POST /api/presences {code, etudiantId} → 201 {id, sessionId, etudiantId, source}.
 * Erreurs du contrat : 400 CODE_INCONNU, 409 DEJA_PRESENT, 410 CODE_EXPIRE.
 * Aucune logique métier ici (B3).
 */
@RestController
@RequestMapping("/api/presences")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping
    public ResponseEntity<PresenceDto> marquer(@Valid @RequestBody MarquerPresenceRequest request) {
        PresenceJpa presence = presenceService.marquer(request.code(), request.etudiantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PresenceDto.de(presence));
    }
}
