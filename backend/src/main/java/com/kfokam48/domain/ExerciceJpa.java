package com.kfokam48.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Exercice déposé (D2). Cycle de vie (D4) : EN_ATTENTE → ASSIGNE → RELU.
 * Le couple (session, étudiant) est unique au niveau SQL (RG17).
 */
@Entity
@Table(name = "exercice")
public class ExerciceJpa {

    public enum Statut { EN_ATTENTE, ASSIGNE, RELU }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Column(nullable = false, length = 2048)
    private String lien;

    @Column(nullable = false, length = 12)
    private String statut;

    @Column(name = "depose_at", nullable = false)
    private Instant deposeAt;

    protected ExerciceJpa() {
        // requis par JPA
    }

    public ExerciceJpa(Long sessionId, Long etudiantId, String lien, Instant deposeAt) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.lien = lien;
        this.statut = Statut.EN_ATTENTE.name();
        this.deposeAt = deposeAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getEtudiantId() {
        return etudiantId;
    }

    public String getLien() {
        return lien;
    }

    public String getStatut() {
        return statut;
    }

    public Instant getDeposeAt() {
        return deposeAt;
    }

    public void assigner() {
        this.statut = Statut.ASSIGNE.name();
    }

    public void marquerRelu() {
        this.statut = Statut.RELU.name();
    }
}
