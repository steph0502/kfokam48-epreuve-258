package com.kfokam48.repository;

import com.kfokam48.domain.SessionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionJpa, Long> {

    /** Unicité du code de présence (RG18) : vérifiée à la génération. */
    boolean existsByCode(String code);
}
