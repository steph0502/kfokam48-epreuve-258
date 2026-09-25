package com.kfokam48.repository;

import com.kfokam48.domain.ExerciceJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciceRepository extends JpaRepository<ExerciceJpa, Long> {

    /** RG17 : un seul exercice par étudiant et par session. */
    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    /** RG14 : exercices en attente d'un relecteur, à assujettir à chaque nouvelle présence. */
    List<ExerciceJpa> findBySessionIdAndStatut(Long sessionId, String statut);

    Optional<ExerciceJpa> findBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
