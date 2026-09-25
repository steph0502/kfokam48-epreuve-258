package com.kfokam48.repository;

import com.kfokam48.domain.PresenceJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<PresenceJpa, Long> {

    /** RG16 : un étudiant ne marque sa présence qu'une fois par session. */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
