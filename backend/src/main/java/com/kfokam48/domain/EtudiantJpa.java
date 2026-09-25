package com.kfokam48.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Étudiant (D2) — choisi par son nom dans une liste, sans mot de passe (Q1, H8). */
@Entity
@Table(name = "etudiant")
public class EtudiantJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    protected EtudiantJpa() {
        // requis par JPA
    }

    public EtudiantJpa(String nom, Long promotionId) {
        this.nom = nom;
        this.promotionId = promotionId;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Long getPromotionId() {
        return promotionId;
    }
}
