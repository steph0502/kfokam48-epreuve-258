package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.ExerciceJpa;
import com.kfokam48.domain.PresenceJpa;
import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.domain.SessionJpa;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.ExerciceRepository;
import com.kfokam48.repository.PresenceRepository;
import com.kfokam48.repository.RelectureRepository;
import com.kfokam48.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (B6) de EF3/EF4 : dépôt (RG17, RG10, lien invalide),
 * assignation immédiate (RG4/RG6/RG7) et retentée RG14 quand un second
 * étudiant se présente.
 */
class ExerciceServiceTest {

    private static final Instant OUVERTURE = Instant.parse("2026-09-25T10:00:00Z");

    private SessionRepository sessions;
    private EtudiantRepository etudiants;
    private ExerciceRepository exercices;
    private PresenceRepository presences;
    private RelectureRepository relectures;
    private ExerciceService service;
    private SessionJpa session;

    @BeforeEach
    void setUp() {
        sessions = mock(SessionRepository.class);
        etudiants = mock(EtudiantRepository.class);
        exercices = mock(ExerciceRepository.class);
        presences = mock(PresenceRepository.class);
        relectures = mock(RelectureRepository.class);

        service = new ExerciceService(sessions, etudiants, exercices, presences, relectures,
                Clock.fixed(OUVERTURE, ZoneOffset.UTC));

        session = new SessionJpa("Cours", 1L, "ABC234", OUVERTURE, OUVERTURE.plus(Duration.ofMinutes(15)));
        when(sessions.findByIdForUpdate(1L)).thenReturn(Optional.of(session));
        when(etudiants.existsById(anyLong())).thenReturn(true);
        when(exercices.save(any(ExerciceJpa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(relectures.save(any(RelectureJpa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void presents(Long... etudiantIds) {
        List<PresenceJpa> liste = java.util.Arrays.stream(etudiantIds)
                .map(id -> new PresenceJpa(1L, id, PresenceJpa.Source.ETUDIANT, OUVERTURE))
                .toList();
        when(presences.findBySessionId(1L)).thenReturn(liste);
    }

    @Test
    void depot_sans_autre_present_reste_EN_ATTENTE_RG14() {
        presents(2L); // seul l'auteur (2) est présent

        ExerciceJpa exercice = service.deposer(1L, 2L, "https://gitlab.com/a/projet");

        assertThat(exercice.getStatut()).isEqualTo(ExerciceJpa.Statut.EN_ATTENTE.name());
        verify(relectures, never()).save(any());
    }

    @Test
    void depot_avec_un_autre_present_assigne_le_relecteur_EF4_RG4() {
        presents(2L, 3L, 4L); // auteur et deux pairs possibles

        ExerciceJpa exercice = service.deposer(1L, 2L, "https://gitlab.com/a/projet");

        assertThat(exercice.getStatut()).isEqualTo(ExerciceJpa.Statut.ASSIGNE.name());
        org.mockito.ArgumentCaptor<RelectureJpa> capteur =
                org.mockito.ArgumentCaptor.forClass(RelectureJpa.class);
        verify(relectures, org.mockito.Mockito.times(2)).save(capteur.capture());
        assertThat(capteur.getAllValues()).extracting(RelectureJpa::getNumeroRelecteur).containsExactly(1, 2);
        assertThat(capteur.getAllValues()).extracting(RelectureJpa::getRelecteurId).containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    void second_depot_409_EXERCICE_DEJA_DEPOSE_RG17() {
        presents(2L, 3L);
        when(exercices.existsBySessionIdAndEtudiantId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.deposer(1L, 2L, "https://exemple.com/x"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(409);
                    assertThat(e.getCode()).isEqualTo("EXERCICE_DEJA_DEPOSE");
                });
    }

    @Test
    void depot_apres_cloture_400_RG10() {
        session.cloturer(OUVERTURE.plus(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> service.deposer(1L, 2L, "https://exemple.com/x"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(400);
                    assertThat(e.getCode()).isEqualTo("SESSION_CLOTUREE");
                });
    }

    @Test
    void lien_non_http_400_LIEN_INVALIDE() {
        presents(2L, 3L);

        assertThatThrownBy(() -> service.deposer(1L, 2L, "ftp://exemple.com/x"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(400);
                    assertThat(e.getCode()).isEqualTo("LIEN_INVALIDE");
                });
        verify(exercices, never()).save(any());
    }

    @Test
    void retente_assignation_quand_un_second_present_arrive_RG14() {
        ExerciceJpa enAttente = new ExerciceJpa(1L, 2L, "https://exemple.com/x", OUVERTURE);
        when(exercices.findBySessionIdAndStatutNot(1L, ExerciceJpa.Statut.RELU.name()))
                .thenReturn(List.of(enAttente));
        when(relectures.findByExerciceIdOrderByNumeroRelecteurAsc(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(), List.of(), List.of(new RelectureJpa(1L, 3L, 1)));

        // D'abord seul présent : rien ne change.
        presents(2L);
        service.retenterAssignations(1L);
        assertThat(enAttente.getStatut()).isEqualTo(ExerciceJpa.Statut.EN_ATTENTE.name());

        // Le pair 3 se présente : première place assignée.
        presents(2L, 3L);
        service.retenterAssignations(1L);
        assertThat(enAttente.getStatut()).isEqualTo(ExerciceJpa.Statut.ASSIGNE.name());
        presents(2L, 3L, 4L);
        service.retenterAssignations(1L);
        org.mockito.ArgumentCaptor<RelectureJpa> capteur = org.mockito.ArgumentCaptor.forClass(RelectureJpa.class);
        verify(relectures, org.mockito.Mockito.times(2)).save(capteur.capture());
        assertThat(capteur.getAllValues()).extracting(RelectureJpa::getNumeroRelecteur).containsExactly(1, 2);
        assertThat(capteur.getAllValues()).extracting(RelectureJpa::getRelecteurId).containsExactly(3L, 4L);
    }
}
