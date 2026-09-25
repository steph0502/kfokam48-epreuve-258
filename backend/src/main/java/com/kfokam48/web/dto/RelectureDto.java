package com.kfokam48.web.dto;

import com.kfokam48.domain.RelectureJpa;

import java.time.Instant;

/**
 * Réponse 200 du POST/PUT /api/relectures/{id}.
 * Ne contient jamais l'identité du relecteur (RG13, Q8) : l'étudiant relu
 * peut voir note et commentaire, pas qui l'a relu.
 */
public record RelectureDto(Long id, Long exerciceId, Integer note, String commentaire, Instant rendueAt) {

    public static RelectureDto de(RelectureJpa relecture) {
        return new RelectureDto(
                relecture.getId(),
                relecture.getExerciceId(),
                relecture.getNote(),
                relecture.getCommentaire(),
                relecture.getRendueAt());
    }
}
