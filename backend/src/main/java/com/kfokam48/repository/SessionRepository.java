package com.kfokam48.repository;

import com.kfokam48.domain.SessionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionJpa, Long> {

    /** Unicité du code de présence (RG18) : vérifiée à la génération. */
    boolean existsByCode(String code);

    /** Résolution d'un code saisi par un étudiant (EF2). */
    java.util.Optional<SessionJpa> findByCode(String code);
}
