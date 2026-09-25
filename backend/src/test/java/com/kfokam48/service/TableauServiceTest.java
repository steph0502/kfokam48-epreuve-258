package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.EtudiantJpa;
import com.kfokam48.domain.ExerciceJpa;
import com.kfokam48.domain.PromotionJpa;
import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.ExerciceRepository;
import com.kfokam48.repository.PresenceRepository;
import com.kfokam48.repository.PromotionRepository;
import com.kfokam48.repository.RelectureRepository;
import com.kfokam48.web.dto.TableauLigneDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires (B6) de EF6 : la moyenne est null sans note reçue (RG15),
 * sinon la moyenne des notes des relectures RENDUES ; compteurs Q16 ;
 * 404 PROMOTION_INCONNUE. Le tableau n'expose jamais le relecteur (RG13).
 */
class TableauServiceTest {

    private static final Instant T0 = Instant.parse("2026-09-25T10:00:00Z");

    private PromotionRepository promotions;
    private EtudiantRepository etudiants;
    private PresenceRepository presences;
    private ExerciceRepository exercices;
    private RelectureRepository relectures;
    private TableauService service;

    @BeforeEach
    void setUp() {
        promotions = mock(PromotionRepository.class);
        etudiants = mock(EtudiantRepository.class);
        presences = mock(PresenceRepository.class);
        exercices = mock(ExerciceRepository.class);
        relectures = mock(RelectureRepository.class);
        service = new TableauService(promotions, etudiants, presences, exercices, relectures);

        when(promotions.findById(1L)).thenReturn(Optional.of(new PromotionJpa("Promo")));
    }

    private EtudiantJpa etudiant(long id, String nom) {
        return new EtudiantJpa(nom, 1L) {
            @Override
            public Long getId() {
                return id;
            }
        };
    }

    private ExerciceJpa exercice(long id, long auteurId) {
        return new ExerciceJpa(1L, auteurId, "https://exemple.com/x", T0) {
            @Override
            public Long getId() {
                return id;
            }
        };
    }

    @Test
    void promotion_inconnue_404() {
        when(promotions.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.tableau(99L)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus().value()).isEqualTo(404);
            assertThat(e.getCode()).isEqualTo("PROMOTION_INCONNUE");
        });
    }

    @Test
    void moyenne_null_si_aucune_note_RG15_et_compteurs_Q16() {
        EtudiantJpa alice = etudiant(1L, "Alice");
        when(etudiants.findByPromotionIdOrderByNomAsc(1L)).thenReturn(List.of(alice));
        when(presences.countByEtudiantId(1L)).thenReturn(2L);
        when(exercices.findByEtudiantId(1L)).thenReturn(List.of(exercice(10L, 1L)));
        when(relectures.findByExerciceIdOrderByNumeroRelecteurAsc(10L)).thenReturn(List.of()); // pas encore relue
        when(relectures.countByRelecteurIdAndRendueAtIsNull(1L)).thenReturn(1L);

        List<TableauLigneDto> lignes = service.tableau(1L);

        assertThat(lignes).hasSize(1);
        TableauLigneDto ligne = lignes.get(0);
        assertThat(ligne.presences()).isEqualTo(2L);
        assertThat(ligne.exercicesDeposes()).isEqualTo(1L);
        assertThat(ligne.moyenne()).isNull(); // RG15
        assertThat(ligne.relecturesEnAttente()).isEqualTo(1L);
    }

    @Test
    void moyenne_des_notes_des_relectures_rendues_uniquement() {
        EtudiantJpa bruno = etudiant(2L, "Bruno");
        when(etudiants.findByPromotionIdOrderByNomAsc(1L)).thenReturn(List.of(bruno));
        when(presences.countByEtudiantId(2L)).thenReturn(1L);
        when(exercices.findByEtudiantId(2L)).thenReturn(List.of(exercice(20L, 2L), exercice(21L, 2L)));

        RelectureJpa rendue = new RelectureJpa(20L, 3L);
        rendue.rendre(10, "ok", T0);
        RelectureJpa enAttente = new RelectureJpa(21L, 4L); // assignée mais non rendue → ignorée
        when(relectures.findByExerciceIdOrderByNumeroRelecteurAsc(20L)).thenReturn(List.of(rendue));
        when(relectures.findByExerciceIdOrderByNumeroRelecteurAsc(21L)).thenReturn(List.of(enAttente));
        when(relectures.countByRelecteurIdAndRendueAtIsNull(2L)).thenReturn(0L);

        TableauLigneDto ligne = service.tableau(1L).get(0);

        assertThat(ligne.moyenne()).isEqualTo(10.0); // seule la relecture RENDUE compte
        assertThat(ligne.moyenneProvisoire()).isTrue(); // un seul avis pour l'exercice noté
    }

    @Test
    void etudiant_sans_exercice_ni_presence() {
        EtudiantJpa chantal = etudiant(3L, "Chantal");
        when(etudiants.findByPromotionIdOrderByNomAsc(1L)).thenReturn(List.of(chantal));
        when(presences.countByEtudiantId(3L)).thenReturn(0L);
        when(exercices.findByEtudiantId(3L)).thenReturn(List.of());
        when(relectures.countByRelecteurIdAndRendueAtIsNull(3L)).thenReturn(0L);

        TableauLigneDto ligne = service.tableau(1L).get(0);

        assertThat(ligne.nom()).isEqualTo("Chantal");
        assertThat(ligne.presences()).isZero();
        assertThat(ligne.exercicesDeposes()).isZero();
        assertThat(ligne.moyenne()).isNull();
        assertThat(ligne.relecturesEnAttente()).isZero();
    }
}
