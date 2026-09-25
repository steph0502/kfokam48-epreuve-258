package com.kfokam48.apierror;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Gestion centralisée des erreurs (contrainte B4) : aucune stack trace ne sort,
 * toute erreur respecte le format imposé {code, message}, y compris les erreurs
 * techniques (payload illisible, ressource inconnue, erreur interne).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Erreurs métier : statut et code viennent de l'exception (ex. 410 CODE_EXPIRE). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> erreurMetier(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    /** Champs manquants ou contraintes @NotNull / @NotBlank violées (ex. 400 champ manquant du contrat). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " : " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiError("REQUETE_INVALIDE", message));
    }

    /** Paramètre de requête manquant (ex. etudiantId sur POST /api/relectures/{id}). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> parametreManquant(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("REQUETE_INVALIDE", "Paramètre manquant : " + ex.getParameterName() + "."));
    }

    /** JSON illisible ou champ de type inattendu. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> requeteIllisible(Exception ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError("REQUETE_INVALIDE", "Requête illisible ou champ de type inattendu."));
    }

    /** Chemin inexistant : même le 404 respecte le format imposé. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> introuvable(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("RESSOURCE_INCONNUE", "Ressource inexistante."));
    }

    /** Filet final (B4) : jamais de stack trace au client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inattendue(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new ApiError("ERREUR_INTERNE", "Erreur interne du serveur."));
    }
}
