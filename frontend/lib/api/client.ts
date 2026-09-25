import { API_BASE } from "./config";
import { ApiError } from "./ApiError";

interface Options {
  method?: "GET" | "POST" | "PUT";
  corps?: unknown;
  /** Paramètres de requête simples, ex. { promotionId: "1" } ou { etudiantId: "5" }. */
  parametres?: Record<string, string>;
}

/**
 * Client HTTP unique de l'application (F3) : tous les appels à l'API passent
 * par ici — sérialisation JSON, gestion des erreurs au format imposé
 * {code, message} et des erreurs réseau, aucun état brut exposé aux écrans.
 */
export async function requete<T>(chemin: string, options: Options = {}): Promise<T> {
  const url = new URL(API_BASE + chemin);
  for (const [cle, valeur] of Object.entries(options.parametres ?? {})) {
    url.searchParams.set(cle, valeur);
  }

  let reponse: Response;
  try {
    reponse = await fetch(url.toString(), {
      method: options.method ?? "GET",
      headers: options.corps !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: options.corps !== undefined ? JSON.stringify(options.corps) : undefined,
      cache: "no-store",
    });
  } catch {
    throw new ApiError("RESEAU_INACCESSIBLE", "Impossible de joindre le serveur — vérifiez que le backend est démarré.");
  }

  if (!reponse.ok) {
    // Le backend renvoie toujours {code, message} — format imposé par le contrat.
    let code = "ERREUR_INATTENDUE";
    let message = `Erreur ${reponse.status} du serveur.`;
    try {
      const corps = (await reponse.json()) as { code?: string; message?: string };
      if (corps.code) code = corps.code;
      if (corps.message) message = corps.message;
    } catch {
      // corps non JSON : on garde le message générique
    }
    throw new ApiError(code, message);
  }

  if (reponse.status === 204) {
    return undefined as T;
  }
  return (await reponse.json()) as T;
}
