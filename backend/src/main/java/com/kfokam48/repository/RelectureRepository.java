package com.kfokam48.repository;

import com.kfokam48.domain.RelectureJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelectureRepository extends JpaRepository<RelectureJpa, Long> {

    /** RG6 : une seule relecture par exercice. */
    boolean existsByExerciceId(Long exerciceId);

    /** EF5 : retrouver la relecture visée par POST /api/relectures/{id}. */
    Optional<RelectureJpa> findById(Long id);

    /** Écran du relecteur (F2) et tableau (Q16) : relectures d'un étudiant. */
    List<RelectureJpa> findByRelecteurId(Long relecteurId);

    List<RelectureJpa> findByRelecteurIdAndRendueAtIsNull(Long relecteurId);
}
