package com.kfokam48.apierror;

/**
 * Format d'erreur imposé par le contrat, pour TOUTES les erreurs sans exception :
 * { "code": "CODE_EXPIRE", "message": "Le code de présence a expiré." }
 * Une stack trace, un corps vide ou la page d'erreur Spring valent zéro (contrainte B4).
 */
public record ApiError(String code, String message) {
}
