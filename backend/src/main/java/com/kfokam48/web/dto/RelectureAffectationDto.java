package com.kfokam48.web.dto;

/** Affectation visible par le pair concerné; aucune identité d’autre relecteur. */
public record RelectureAffectationDto(
        Long id,
        Long exerciceId,
        String auteurNom,
        String lien,
        boolean rendue) {
}
