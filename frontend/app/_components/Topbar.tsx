import Link from "next/link";

/**
 * Barre de navigation partagée — présentation uniquement.
 * Brand mark dégradé reprenant la charte de « Gestion de Stock ».
 */
export function Topbar({ active }: { active: "accueil" | "formateur" | "etudiant" | "relecteur" }) {
  const liens = [
    { href: "/", label: "Accueil", cle: "accueil" as const },
    { href: "/formateur", label: "Espace formateur", cle: "formateur" as const },
    { href: "/etudiant", label: "Espace étudiant", cle: "etudiant" as const },
    { href: "/relecteur", label: "Espace relecteur", cle: "relecteur" as const },
  ];

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <Link href="/" className="brand">
          <span className="brand-mark">K48</span>
          <span className="brand-text">
            <b>KFOKAM48</b>
            <small>Présences &amp; relecture</small>
          </span>
        </Link>
        <nav className="topbar-nav">
          {liens.map((l) => (
            <Link key={l.cle} href={l.href} className={`nav-link${active === l.cle ? " active" : ""}`}>
              {l.label}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}
