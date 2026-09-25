package com.kfokam48.repository;

import com.kfokam48.domain.EtudiantJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<EtudiantJpa, Long> {
}
