package com.kfokam48.web.dto;

import java.util.List;

/** Note retenue et retours anonymisés visibles par l’auteur de l’exercice. */
public record EtudiantExerciceDto(
        Long id,
        Long sessionId,
        String lien,
        String statut,
        Double noteRetenue,
        boolean noteProvisoire,
        List<EvaluationExerciceDto> evaluations) {
}
