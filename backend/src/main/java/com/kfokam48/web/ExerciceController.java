package com.kfokam48.web;

import com.kfokam48.domain.ExerciceJpa;
import com.kfokam48.service.ExerciceService;
import com.kfokam48.web.dto.DeposerExerciceRequest;
import com.kfokam48.web.dto.ExerciceDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EF3/EF4 — POST /api/exercices {sessionId, etudiantId, lien} → 201 {id, statut}.
 * statut vaut ASSIGNE si au moins un relecteur a pu être désigné, EN_ATTENTE sinon (RG14).
 * Erreurs du contrat : 400 LIEN_INVALIDE, 409 EXERCICE_DEJA_DEPOSE.
 * Aucune logique métier ici (B3).
 */
@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService exerciceService;

    public ExerciceController(ExerciceService exerciceService) {
        this.exerciceService = exerciceService;
    }

    @PostMapping
    public ResponseEntity<ExerciceDto> deposer(@Valid @RequestBody DeposerExerciceRequest request) {
        ExerciceJpa exercice = exerciceService.deposer(request.sessionId(), request.etudiantId(), request.lien());
        return ResponseEntity.status(HttpStatus.CREATED).body(ExerciceDto.de(exercice));
    }
}
