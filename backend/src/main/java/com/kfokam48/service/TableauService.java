package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.EtudiantJpa;
import com.kfokam48.domain.ExerciceJpa;
import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.ExerciceRepository;
import com.kfokam48.repository.PresenceRepository;
import com.kfokam48.repository.PromotionRepository;
import com.kfokam48.repository.RelectureRepository;
import com.kfokam48.web.dto.TableauLigneDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

/**
 * Tableau récapitulatif du formateur (EF6, Q16) : par étudiant de la promotion,
 * ses présences, ses exercices déposés, la moyenne des notes reçues
 * (null si aucune — RG15) et ses relectures pas encore rendues.
 * Le tableau n'expose jamais l'identité des relecteurs (RG13).
 */
@Service
public class TableauService {

    private final PromotionRepository promotions;
    private final EtudiantRepository etudiants;
    private final PresenceRepository presences;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;

    public TableauService(PromotionRepository promotions, EtudiantRepository etudiants,
                          PresenceRepository presences, ExerciceRepository exercices,
                          RelectureRepository relectures) {
        this.promotions = promotions;
        this.etudiants = etudiants;
        this.presences = presences;
        this.exercices = exercices;
        this.relectures = relectures;
    }

    @Transactional(readOnly = true)
    public List<TableauLigneDto> tableau(Long promotionId) {
        promotions.findById(promotionId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE",
                "La promotion " + promotionId + " n'existe pas."));
        return etudiants.findByPromotionIdOrderByNomAsc(promotionId).stream()
                .map(this::ligne)
                .toList();
    }

    private TableauLigneDto ligne(EtudiantJpa etudiant) {
        List<ExerciceJpa> deposés = exercices.findByEtudiantId(etudiant.getId());
        List<Double> notesRetenues = new ArrayList<>();
        boolean moyenneProvisoire = false;
        for (ExerciceJpa exercice : deposés) {
            List<Integer> notesExercice = relectures
                    .findByExerciceIdOrderByNumeroRelecteurAsc(exercice.getId()).stream()
                    .filter(RelectureJpa::estRendue)
                    .map(RelectureJpa::getNote)
                    .toList();
            if (!notesExercice.isEmpty()) {
                notesRetenues.add(notesExercice.stream().mapToInt(Integer::intValue).average().orElse(0.0));
                moyenneProvisoire |= notesExercice.size() == 1;
            }
        }
        Double moyenne = notesRetenues.isEmpty() ? null // RG15 : null sans note reçue
                : notesRetenues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return new TableauLigneDto(
                etudiant.getId(),
                etudiant.getNom(),
                presences.countByEtudiantId(etudiant.getId()),
                deposés.size(),
                moyenne,
                moyenneProvisoire,
                relectures.countByRelecteurIdAndRendueAtIsNull(etudiant.getId()));
    }
}
