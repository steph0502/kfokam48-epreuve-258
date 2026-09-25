import Link from "next/link";
import { Topbar } from "./_components/Topbar";

export default function Accueil() {
  return (
    <>
      <Topbar active="accueil" />
      <main className="container">
        <section className="hero">
          <span className="hero-badge">KFOKAM48</span>
          <h1>Présences &amp; relecture par les pairs</h1>
          <p>
            Un code de présence à projeter, un dépôt de lien d&apos;exercice, une relecture
            attribuée au hasard parmi les présents — sans compte ni mot de passe.
          </p>
        </section>

        <div className="space-grid">
          <Link href="/formateur" className="space-card">
            <span className="space-icon">F</span>
            <h2>Espace formateur</h2>
            <p>
              Ouvrir une session et projeter le code de présence, clôturer la session,
              consulter le tableau récapitulatif de la promotion.
            </p>
            <span className="space-arrow">Ouvrir l&apos;espace →</span>
          </Link>

          <Link href="/etudiant" className="space-card">
            <span className="space-icon">E</span>
            <h2>Espace étudiant</h2>
            <p>
              Choisir son nom dans la liste, marquer sa présence avec le code affiché,
              déposer le lien de son exercice pour la session.
            </p>
            <span className="space-arrow">Ouvrir l&apos;espace →</span>
          </Link>
          <Link href="/relecteur" className="space-card">
            <span className="space-icon">R</span>
            <h2>Espace relecteur</h2>
            <p>
              Consulter les exercices attribués, rendre une note et un commentaire pour chaque pair.
            </p>
            <span className="space-arrow">Ouvrir l&apos;espace →</span>
          </Link>
        </div>
      </main>
      <footer className="app-footer">
        KFOKAM48 — Direction de la formation · Données de démonstration au démarrage
      </footer>
    </>
  );
}
