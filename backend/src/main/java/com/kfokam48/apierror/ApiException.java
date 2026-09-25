package com.kfokam48.apierror;

import org.springframework.http.HttpStatus;

/**
 * Erreur métier transportant le couple (statut HTTP, code stable) attendu par le contrat.
 * Exemple : new ApiException(HttpStatus.GONE, "CODE_EXPIRE", "Le code de présence a expiré.");
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
