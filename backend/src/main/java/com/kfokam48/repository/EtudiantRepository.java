package com.kfokam48.repository;

import com.kfokam48.domain.EtudiantJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtudiantRepository extends JpaRepository<EtudiantJpa, Long> {

    /** Tableau du formateur (EF6), ordonné par nom pour un affichage stable. */
    List<EtudiantJpa> findByPromotionIdOrderByNomAsc(Long promotionId);
}
