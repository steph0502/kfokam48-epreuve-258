package com.kfokam48.web.dto;

import com.kfokam48.domain.ExerciceJpa;

/** Réponse 201 du POST /api/exercices — exactement {id, statut} (contrat). */
public record ExerciceDto(Long id, String statut) {

    public static ExerciceDto de(ExerciceJpa exercice) {
        return new ExerciceDto(exercice.getId(), exercice.getStatut());
    }
}
