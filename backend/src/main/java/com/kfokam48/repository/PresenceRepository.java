package com.kfokam48.repository;

import com.kfokam48.domain.PresenceJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresenceRepository extends JpaRepository<PresenceJpa, Long> {

    /** RG16 : un étudiant ne marque sa présence qu'une fois par session. */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    /** RG6/RG7 : les présents de la session, parmi lesquels choisir le relecteur. */
    List<PresenceJpa> findBySessionId(Long sessionId);

    /** Tableau (Q16) : nombre de présences d'un étudiant, toutes sessions confondues. */
    long countByEtudiantId(Long etudiantId);
}
