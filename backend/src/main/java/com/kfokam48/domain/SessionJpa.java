package com.kfokam48.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Session de cours (D2). clotureAt reste null tant que le formateur n'a pas
 * clôturé (Q3, Q12). Le code expire 15 minutes après l'ouverture (RG1) et est
 * unique (RG18, H6).
 */
@Entity
@Table(name = "session")
public class SessionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(name = "promotion_id", nullable = false)
    private Long promotionId;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private Instant ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private Instant expirationAt;

    @Column(name = "cloture_at")
    private Instant clotureAt;

    protected SessionJpa() {
        // requis par JPA
    }

    public SessionJpa(String titre, Long promotionId, String code, Instant ouvertureAt, Instant expirationAt) {
        this.titre = titre;
        this.promotionId = promotionId;
        this.code = code;
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = expirationAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public Long getPromotionId() {
        return promotionId;
    }

    public String getCode() {
        return code;
    }

    public Instant getOuvertureAt() {
        return ouvertureAt;
    }

    public Instant getExpirationAt() {
        return expirationAt;
    }

    public Instant getClotureAt() {
        return clotureAt;
    }

    public void cloturer(Instant maintenant) {
        this.clotureAt = maintenant;
    }
}
