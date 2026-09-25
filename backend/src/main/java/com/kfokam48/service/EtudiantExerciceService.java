package com.kfokam48.service;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.domain.ExerciceJpa;
import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.ExerciceRepository;
import com.kfokam48.repository.RelectureRepository;
import com.kfokam48.web.dto.EtudiantExerciceDto;
import com.kfokam48.web.dto.EvaluationExerciceDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lecture par un étudiant de ses exercices et évaluations anonymisées (EF10/RG13). */
@Service
public class EtudiantExerciceService {

    private final EtudiantRepository etudiants;
    private final ExerciceRepository exercices;
    private final RelectureRepository relectures;

    public EtudiantExerciceService(EtudiantRepository etudiants,
                                   ExerciceRepository exercices,
                                   RelectureRepository relectures) {
        this.etudiants = etudiants;
        this.exercices = exercices;
        this.relectures = relectures;
    }

    @Transactional(readOnly = true)
    public List<EtudiantExerciceDto> lire(Long etudiantId) {
        if (!etudiants.existsById(etudiantId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU",
                    "L’étudiant " + etudiantId + " n’existe pas.");
        }
        return exercices.findByEtudiantId(etudiantId).stream().map(this::dto).toList();
    }

    private EtudiantExerciceDto dto(ExerciceJpa exercice) {
        List<EvaluationExerciceDto> evaluations = relectures
                .findByExerciceIdOrderByNumeroRelecteurAsc(exercice.getId()).stream()
                .filter(RelectureJpa::estRendue)
                .map(r -> new EvaluationExerciceDto(r.getNote(), r.getCommentaire()))
                .toList();
        Double noteRetenue = evaluations.isEmpty() ? null
                : evaluations.stream().mapToInt(EvaluationExerciceDto::note).average().orElse(0.0);
        return new EtudiantExerciceDto(
                exercice.getId(), exercice.getSessionId(), exercice.getLien(), exercice.getStatut(),
                noteRetenue, evaluations.size() == 1, evaluations);
    }
}
