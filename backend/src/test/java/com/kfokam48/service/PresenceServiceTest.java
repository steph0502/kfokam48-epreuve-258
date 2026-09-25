package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.PresenceJpa;
import com.kfokam48.domain.SessionJpa;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.PresenceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (B6) de EF2 avec horloge figée : la limite exacte de RG1
 * (valable jusqu'à expirationAt, expiré une seconde après), RG2 (clôture),
 * RG16 (unicité) et le code inconnu. En unitaire, l'entité n'est pas persistée :
 * l'identifiant de session est vérifié par référence (l'intégration prouve les vrais ids).
 */
class PresenceServiceTest {

    private static final String CODE = "ABC234";
    private static final Instant OUVERTURE = Instant.parse("2026-09-25T10:00:00Z");
    private static final Instant EXPIRATION = OUVERTURE.plus(Duration.ofMinutes(15));

    private SessionRepository sessions;
    private EtudiantRepository etudiants;
    private PresenceRepository presences;
    private ExerciceService exerciceService;

    @BeforeEach
    void setUp() {
        sessions = mock(SessionRepository.class);
        etudiants = mock(EtudiantRepository.class);
        presences = mock(PresenceRepository.class);
        exerciceService = mock(ExerciceService.class);
    }

    private PresenceService service(Clock horloge) {
        return new PresenceService(sessions, etudiants, presences, exerciceService, horloge);
    }

    private SessionJpa sessionValable() {
        SessionJpa session = new SessionJpa("Cours", 1L, CODE, OUVERTURE, EXPIRATION);
        when(sessions.findByCode(CODE)).thenReturn(Optional.of(session));
        when(etudiants.existsById(5L)).thenReturn(true);
        return session;
    }

    private void echoueAvec(Runnable appel, int statut, String codeAttendu) {
        assertThatThrownBy(appel::run)
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(statut);
                    assertThat(e.getCode()).isEqualTo(codeAttendu);
                });
    }

    @Test
    void code_inconnu_400() {
        sessionValable();
        when(sessions.findByCode("ZZZZZZ")).thenReturn(Optional.empty());

        echoueAvec(() -> service(Clock.fixed(OUVERTURE, ZoneOffset.UTC)).marquer("ZZZZZZ", 5L),
                400, "CODE_INCONNU");
        verify(presences, never()).save(any());
    }

    @Test
    void session_cloturee_400_RG2_meme_si_code_valable() {
        SessionJpa cloturee = sessionValable();
        cloturee.cloturer(OUVERTURE.plus(Duration.ofMinutes(10)));

        echoueAvec(() -> service(Clock.fixed(OUVERTURE, ZoneOffset.UTC)).marquer(CODE, 5L),
                400, "SESSION_CLOTUREE");
        verify(presences, never()).save(any());
    }

    @Test
    void etudiant_inconnu_400_et_rien_n_est_enregistre() {
        sessionValable();
        when(etudiants.existsById(99L)).thenReturn(false);

        echoueAvec(() -> service(Clock.fixed(OUVERTURE, ZoneOffset.UTC)).marquer(CODE, 99L),
                400, "ETUDIANT_INCONNU");
        verify(presences, never()).save(any());
    }

    @Test
    void code_expire_410_RG1_une_seconde_apres_la_fenetre() {
        sessionValable();
        Clock uneSecondeApres = Clock.fixed(EXPIRATION.plusSeconds(1), ZoneOffset.UTC);

        echoueAvec(() -> service(uneSecondeApres).marquer(CODE, 5L), 410, "CODE_EXPIRE");
        verify(presences, never()).save(any());
    }

    @Test
    void nominal_201_source_etudiant() {
        SessionJpa session = sessionValable();
        when(presences.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Clock aLaLimite = Clock.fixed(EXPIRATION, ZoneOffset.UTC);

        PresenceJpa presence = service(aLaLimite).marquer(CODE, 5L);

        assertThat(presence.getSessionId()).isEqualTo(session.getId());
        assertThat(presence.getEtudiantId()).isEqualTo(5L);
        assertThat(presence.getSource()).isEqualTo("ETUDIANT");
        assertThat(presence.getCreeAt()).isEqualTo(EXPIRATION);
        // RG14 : une présence déclenche la retentée d'assignation de sa session.
        verify(exerciceService).retenterAssignations(session.getId());
    }

    @Test
    void deja_present_409_RG16() {
        sessionValable();
        when(presences.existsBySessionIdAndEtudiantId(any(), eq(5L))).thenReturn(true);

        echoueAvec(() -> service(Clock.fixed(OUVERTURE, ZoneOffset.UTC)).marquer(CODE, 5L),
                409, "DEJA_PRESENT");
        verify(presences, never()).save(any());
    }
}
