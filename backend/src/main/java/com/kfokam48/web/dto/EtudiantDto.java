package com.kfokam48.web.dto;

import com.kfokam48.domain.EtudiantJpa;

/** Étudiant du contrat {id, nom} — liste pour choisir son nom (EF11, Q1). */
public record EtudiantDto(Long id, String nom) {

    public static EtudiantDto de(EtudiantJpa etudiant) {
        return new EtudiantDto(etudiant.getId(), etudiant.getNom());
    }
}
