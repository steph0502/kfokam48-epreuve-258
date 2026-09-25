package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.ExerciceJpa;
import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.domain.SessionJpa;
import com.kfokam48.repository.ExerciceRepository;
import com.kfokam48.repository.RelectureRepository;
import com.kfokam48.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (B6) de EF5 : RG7 (note), RG4 (auto-relecture, prioritaire),
 * relecteur non assigné, RG8 (seconde création 409), correction Q10/H9 fermée
 * après clôture (Q15) et H10 (soumission initiale encore possible après clôture).
 */
class RelectureServiceTest {

    private static final Instant OUVERTURE = Instant.parse("2026-09-25T10:00:00Z");

    private RelectureRepository relectures;
    private ExerciceRepository exercices;
    private SessionRepository sessions;
    private RelectureService service;
    private RelectureJpa relecture;
    private RelectureJpa secondeRelecture;
    private ExerciceJpa exercice;
    private SessionJpa session;

    @BeforeEach
    void setUp() {
        relectures = mock(RelectureRepository.class);
        exercices = mock(ExerciceRepository.class);
        sessions = mock(SessionRepository.class);
        service = new RelectureService(relectures, exercices, sessions,
                Clock.fixed(OUVERTURE, ZoneOffset.UTC));

        // Auteur = 2, relecteur assigné = 3, exercice ASSIGNE, session ouverte.
        relecture = new RelectureJpa(10L, 3L, 1);
        secondeRelecture = new RelectureJpa(10L, 4L, 2);
        secondeRelecture.rendre(13, "Avis pair.", OUVERTURE);
        when(relectures.findByExerciceIdOrderByNumeroRelecteurAsc(10L))
                .thenReturn(java.util.List.of(relecture, secondeRelecture));
        exercice = new ExerciceJpa(1L, 2L, "https://exemple.com/x", OUVERTURE);
        exercice.assigner();
        session = new SessionJpa("Cours", 1L, "ABC234", OUVERTURE, OUVERTURE.plus(Duration.ofMinutes(15)));

        when(relectures.findById(7L)).thenReturn(Optional.of(relecture));
        when(exercices.findById(10L)).thenReturn(Optional.of(exercice));
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
    }

    private void echoueAvec(Runnable appel, int statut, String codeAttendu) {
        assertThatThrownBy(appel::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus().value()).isEqualTo(statut);
            assertThat(e.getCode()).isEqualTo(codeAttendu);
        });
    }

    @Test
    void nominal_le_relecteur_rend_200_et_exercice_RELU() {
        service.rendre(7L, 3L, 15, "Bon travail.");
        assertThat(relecture.estRendue()).isTrue();
        assertThat(relecture.getNote()).isEqualTo(15);
        assertThat(exercice.getStatut()).isEqualTo(ExerciceJpa.Statut.RELU.name());
    }

    @Test
    void note_hors_bornes_400_NOTE_INVALIDE_RG7() {
        echoueAvec(() -> service.rendre(7L, 3L, 21, "x"), 400, "NOTE_INVALIDE");
        echoueAvec(() -> service.rendre(7L, 3L, -1, "x"), 400, "NOTE_INVALIDE");
        echoueAvec(() -> service.rendre(7L, 3L, null, "x"), 400, "NOTE_INVALIDE");
    }

    @Test
    void auto_relecture_403_prioritaire_RG4() {
        // L'auteur (2) tente de rendre : 403 même s'il n'est pas le relecteur assigné.
        echoueAvec(() -> service.rendre(7L, 2L, 10, "x"), 403, "AUTO_RELECTURE");
    }

    @Test
    void relecteur_non_assigne_403() {
        echoueAvec(() -> service.rendre(7L, 4L, 10, "x"), 403, "RELECTEUR_NON_ASSIGNE");
    }

    @Test
    void premiere_note_seule_reste_provisoire_et_exercice_ASSIGNE() {
        when(relectures.findByExerciceIdOrderByNumeroRelecteurAsc(10L)).thenReturn(java.util.List.of(relecture));
        service.rendre(7L, 3L, 15, "Bon travail.");
        assertThat(exercice.getStatut()).isEqualTo(ExerciceJpa.Statut.ASSIGNE.name());
    }

    @Test
    void seconde_soumission_409_RELECTURE_DEJA_RENDUE_RG8() {
        service.rendre(7L, 3L, 15, "Première version.");
        echoueAvec(() -> service.rendre(7L, 3L, 18, "Seconde tentative."), 409, "RELECTURE_DEJA_RENDUE");
        assertThat(relecture.getNote()).isEqualTo(15); // inchangée
    }

    @Test
    void correction_Q10_possible_avant_cloture_PUT() {
        service.rendre(7L, 3L, 15, "Première version.");
        RelectureJpa corrigee = service.corriger(7L, 3L, 17, "Version affinée.");
        assertThat(corrigee.getNote()).isEqualTo(17);
        assertThat(corrigee.getRendueAt()).isEqualTo(OUVERTURE); // date initiale conservée
    }

    @Test
    void correction_apres_cloture_409_CORRECTION_INTERDITE_Q15() {
        service.rendre(7L, 3L, 15, "Rendu.");
        session.cloturer(OUVERTURE.plus(Duration.ofMinutes(30)));
        echoueAvec(() -> service.corriger(7L, 3L, 18, "Trop tard."), 409, "CORRECTION_INTERDITE");
        assertThat(relecture.getNote()).isEqualTo(15);
    }

    @Test
    void soumission_initiale_encore_possible_apres_cloture_H10() {
        session.cloturer(OUVERTURE.plus(Duration.ofMinutes(30)));
        service.rendre(7L, 3L, 12, "Tardif mais possible (H10).");
        assertThat(relecture.estRendue()).isTrue();
        assertThat(exercice.getStatut()).isEqualTo(ExerciceJpa.Statut.RELU.name());
    }

    @Test
    void relecture_inconnue_404() {
        when(relectures.findById(99L)).thenReturn(Optional.empty());
        echoueAvec(() -> service.rendre(99L, 3L, 10, "x"), 404, "RELECTURE_INCONNUE");
    }
}
