package com.kfokam48.repository;

import com.kfokam48.domain.SessionJpa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionJpa, Long> {

    /** Unicité du code de présence (RG18) : vérifiée à la génération. */
    boolean existsByCode(String code);

    /** Le verrou sérialise les marquages simultanés d’une même session (EF2). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<SessionJpa> findByCode(String code);

    /** Verrouille la session avant tout dépôt qui peut modifier ses affectations. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionJpa s where s.id = :id")
    java.util.Optional<SessionJpa> findByIdForUpdate(@Param("id") Long id);
}
