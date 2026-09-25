package com.kfokam48.web;

import com.kfokam48.domain.SessionJpa;
import com.kfokam48.service.SessionService;
import com.kfokam48.web.dto.OuvrirSessionRequest;
import com.kfokam48.web.dto.SessionDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EF1 — POST /api/sessions {titre, promotionId} → 201 {id, code, ouvertureAt, expirationAt}.
 * Aucune logique métier ici (B3) : elle vit dans SessionService.
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionDto> ouvrir(@Valid @RequestBody OuvrirSessionRequest request) {
        SessionJpa session = sessionService.ouvrir(request.titre(), request.promotionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionDto.de(session));
    }
}
