package com.kfokam48.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corps du POST/PUT /api/relectures/{id} — le contrat exige {note, commentaire}.
 * La borne 0–20 n'est PAS validée ici : RG7 est portée par le service, qui seul
 * produit le code imposé NOTE_INVALIDE (une seule source de vérité).
 */
public record RendreRelectureRequest(
        @NotNull Integer note,
        @NotBlank String commentaire) {
}
