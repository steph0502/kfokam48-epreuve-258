/**
 * Erreur d'API normalisée : porte le code d'erreur du backend
 * (format imposé {code, message}) pour un affichage lisible à l'écran.
 * Aucun appel fetch dispersé ailleurs dans l'application (F3).
 */
export class ApiError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.code = code;
  }
}
