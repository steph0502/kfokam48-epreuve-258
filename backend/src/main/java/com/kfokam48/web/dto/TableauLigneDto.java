package com.kfokam48.web.dto;

/**
 * Une ligne du tableau récapitulatif (EF6, Q16) — exactement le schéma du contrat :
 * {etudiantId, nom, presences, exercicesDeposes, moyenne, relecturesEnAttente}.
 * moyenne est null sans note ; moyenneProvisoire indique si au moins un exercice a un seul avis rendu (RG15/RG19).
 */
public record TableauLigneDto(
        Long etudiantId,
        String nom,
        long presences,
        long exercicesDeposes,
        Double moyenne,
        boolean moyenneProvisoire,
        long relecturesEnAttente) {
}
