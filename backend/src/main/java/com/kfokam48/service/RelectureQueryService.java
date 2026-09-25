package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.ExerciceRepository;
import com.kfokam48.repository.RelectureRepository;
import com.kfokam48.web.dto.RelectureAffectationDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Liste les affectations d’un pair pour son écran de relecture (EF5/F2). */
@Service
public class RelectureQueryService {

    private final EtudiantRepository etudiants;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;

    public RelectureQueryService(EtudiantRepository etudiants,
                                 ExerciceRepository exercices,
                                 RelectureRepository relectures) {
        this.etudiants = etudiants;
        this.exercices = exercices;
        this.relectures = relectures;
    }

    @Transactional(readOnly = true)
    public List<RelectureAffectationDto> lister(Long etudiantId, boolean rendue) {
        if (!etudiants.existsById(etudiantId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU",
                    "L’étudiant " + etudiantId + " n’existe pas.");
        }
        List<RelectureJpa> affectations = rendue
                ? relectures.findByRelecteurIdAndRendueAtIsNotNull(etudiantId)
                : relectures.findByRelecteurIdAndRendueAtIsNull(etudiantId);
        return affectations.stream().map(relecture -> {
            var exercice = exercices.findById(relecture.getExerciceId()).orElseThrow();
            var auteur = etudiants.findById(exercice.getEtudiantId()).orElseThrow();
            return new RelectureAffectationDto(
                    relecture.getId(), exercice.getId(), auteur.getNom(), exercice.getLien(), relecture.estRendue());
        }).toList();
    }
}
