import Link from "next/link";

export default function Accueil() {
  return (
    <main>
      <h1>KFOKAM48 — Présences &amp; relecture par les pairs</h1>
      <p>Choisissez votre espace :</p>
      <ul>
        <li>
          <Link href="/formateur">Espace formateur</Link> — ouvrir une session, clôturer, tableau récapitulatif
        </li>
        <li>
          <Link href="/etudiant">Espace étudiant</Link> — choisir son nom, marquer sa présence, déposer son exercice
        </li>
      </ul>
    </main>
  );
}
