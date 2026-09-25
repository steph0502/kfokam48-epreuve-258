/**
 * URL de l'API, configurable par variable d'environnement Next.js
 * (NEXT_PUBLIC_API_URL), défaut : backend local sur le port 8080.
 */
export const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
