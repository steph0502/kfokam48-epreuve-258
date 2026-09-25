package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.codegenerator.CodeGenerator;
import com.kfokam48.domain.PromotionJpa;
import com.kfokam48.domain.SessionJpa;
import com.kfokam48.repository.PromotionRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (B6) de la règle RG1 : expirationAt = ouvertureAt + 15 minutes,
 * vérifiée avec une horloge figée — plus la garde promotion inconnue et la
 * régénération du code en cas de collision (RG18).
 */
class SessionServiceTest {

    private SessionRepository sessions;
    private PromotionRepository promotions;
    private CodeGenerator generateur;
    private SessionService service;
    private Instant maintenant;

    @BeforeEach
    void setUp() {
        sessions = mock(SessionRepository.class);
        promotions = mock(PromotionRepository.class);
        generateur = mock(CodeGenerator.class);
        maintenant = Instant.parse("2026-09-25T10:00:00Z");
        Clock horlogeFigee = Clock.fixed(maintenant, ZoneOffset.UTC);
        service = new SessionService(sessions, promotions, generateur, horlogeFigee);

        when(promotions.findById(1L)).thenReturn(Optional.of(new PromotionJpa("Promo test")));
        when(sessions.save(any(SessionJpa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(generateur.nouveauCode()).thenReturn("ABC234", "DEF567", "GHI678");
    }

    @Test
    void expiration_exactement_a_15_minutes_RG1() {
        SessionJpa session = service.ouvrir("Algorithmique", 1L);

        assertThat(session.getOuvertureAt()).isEqualTo(maintenant);
        assertThat(session.getExpirationAt()).isEqualTo(maintenant.plus(Duration.ofMinutes(15)));
    }

    @Test
    void promotion_inconnue_rejetee_400_et_rien_n_est_enregistre() {
        assertThatThrownBy(() -> service.ouvrir("Cours sans promo", 99L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(400);
                    assertThat(e.getCode()).isEqualTo("PROMOTION_INCONNUE");
                });
        verify(sessions, never()).save(any());
    }

    @Test
    void collision_de_code_RG18_le_code_est_regenere() {
        when(sessions.existsByCode("ABC234")).thenReturn(true);
        when(sessions.existsByCode("DEF567")).thenReturn(false);

        SessionJpa session = service.ouvrir("Cours 3", 1L);

        assertThat(session.getCode()).isEqualTo("DEF567");
    }

    @Test
    void cloture_200_date_posee_EF7() {
        SessionJpa session = service.ouvrir("Cours", 1L);
        when(sessions.findById(1L)).thenReturn(Optional.of(session));

        SessionJpa cloturee = service.cloturer(1L);

        assertThat(cloturee.getClotureAt()).isEqualTo(maintenant);
    }

    @Test
    void double_cloture_400_DEJA_CLOTUREE() {
        SessionJpa session = service.ouvrir("Cours", 1L);
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
        service.cloturer(1L);

        assertThatThrownBy(() -> service.cloturer(1L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(400);
                    assertThat(e.getCode()).isEqualTo("DEJA_CLOTUREE");
                });
    }

    @Test
    void cloture_session_inconnue_404() {
        when(sessions.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cloturer(99L))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus().value()).isEqualTo(404);
                    assertThat(e.getCode()).isEqualTo("SESSION_INCONNUE");
                });
    }
}
