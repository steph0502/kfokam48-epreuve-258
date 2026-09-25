package com.kfokam48.web.dto;

/**
 * Une ligne du tableau récapitulatif (EF6, Q16) — exactement le schéma du contrat :
 * {etudiantId, nom, presences, exercicesDeposes, moyenne, relecturesEnAttente}.
 * moyenne est null si aucune note reçue (RG15).
 */
public record TableauLigneDto(
        Long etudiantId,
        String nom,
        long presences,
        long exercicesDeposes,
        Double moyenne,
        long relecturesEnAttente) {
}
