package com.kfokam48.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Relecture (D2). Deux affectations maximum par exercice (RG5/RG6, unicité SQL). note/commentaire/
 * rendueAt restent null tant que le relecteur n'a pas rendu (EF4 → EF5).
 * Le relecteur est un étudiant (pas d'entité Relecteur — décision §2 du CDC, Q7).
 */
@Entity
@Table(name = "relecture")
public class RelectureJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercice_id", nullable = false)
    private Long exerciceId;

    @Column(name = "relecteur_id", nullable = false)
    private Long relecteurId;

    @Column(name = "numero_relecteur", nullable = false)
    private Integer numeroRelecteur;

    @Column
    private Integer note;

    @Column(length = 2000)
    private String commentaire;

    @Column(name = "rendue_at")
    private Instant rendueAt;

    protected RelectureJpa() {
        // requis par JPA
    }

    public RelectureJpa(Long exerciceId, Long relecteurId) {
        this(exerciceId, relecteurId, 1);
    }

    public RelectureJpa(Long exerciceId, Long relecteurId, Integer numeroRelecteur) {
        this.exerciceId = exerciceId;
        this.relecteurId = relecteurId;
        this.numeroRelecteur = numeroRelecteur;
    }

    public Long getId() {
        return id;
    }

    public Long getExerciceId() {
        return exerciceId;
    }

    public Long getRelecteurId() {
        return relecteurId;
    }

    public Integer getNumeroRelecteur() {
        return numeroRelecteur;
    }

    public Integer getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public Instant getRendueAt() {
        return rendueAt;
    }

    public boolean estRendue() {
        return rendueAt != null;
    }

    public void rendre(Integer note, String commentaire, Instant maintenant) {
        this.note = note;
        this.commentaire = commentaire;
        this.rendueAt = maintenant;
    }

    /** Correction Q10/H9 : met à jour la version courante sans toucher à la date de rendu initiale. */
    public void corriger(Integer note, String commentaire) {
        this.note = note;
        this.commentaire = commentaire;
    }
}
