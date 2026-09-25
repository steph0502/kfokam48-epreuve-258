package com.kfokam48.web.dto;

/** Une note rendue sur un exercice, sans identité du pair qui l’a attribuée. */
public record EvaluationExerciceDto(Integer note, String commentaire) {
}
