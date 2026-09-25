# Frontend — KFOKAM48

Next.js (React 19, TypeScript). Le framework est justifié dans le `README`
à la racine : routing par fichier intégré, rendu hybride SSR/CSR, un seul
outil de build — le trio d'écrans s'assemble sans configuration de routing.

## Démarrage

```bash
npm install
npm run dev
```

L'interface tourne sur http://localhost:3000 et appelle l'API sur
http://localhost:8080 (variable `NEXT_PUBLIC_API_URL` pour surcharger).

## Structure

- `app/` — pages (routing par fichier) : `/` accueil, `/formateur`, `/etudiant`, `/relecteur`
- `lib/api/` — **couche API dédiée (F3)** : client HTTP unique, erreurs
  normalisées `{code, message}`, endpoints typés alignés sur `api/contrat.yaml`.
  Aucun `fetch` en dehors de `lib/api/`, aucune règle métier dupliquée :
  la moyenne affichée vient de l'API (RG15).
