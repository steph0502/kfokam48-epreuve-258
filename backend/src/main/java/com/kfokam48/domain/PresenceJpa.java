package com.kfokam48.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Présence (D2). Le couple (session, étudiant) est unique au niveau SQL (RG16).
 * source ∈ {ETUDIANT, FORMATEUR} — champ imposé par le contrat (Q14).
 */
@Entity
@Table(name = "presence")
public class PresenceJpa {

    public enum Source { ETUDIANT, FORMATEUR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long etudiantId;

    @Column(nullable = false, length = 10)
    private String source;

    @Column(name = "cree_at", nullable = false)
    private Instant creeAt;

    protected PresenceJpa() {
        // requis par JPA
    }

    public PresenceJpa(Long sessionId, Long etudiantId, Source source, Instant creeAt) {
        this.sessionId = sessionId;
        this.etudiantId = etudiantId;
        this.source = source.name();
        this.creeAt = creeAt;
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

    public String getSource() {
        return source;
    }

    public Instant getCreeAt() {
        return creeAt;
    }
}
