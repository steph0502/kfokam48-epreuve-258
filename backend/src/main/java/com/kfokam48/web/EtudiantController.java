package com.kfokam48.web;

import com.kfokam48.apierror.ApiException;
import com.kfokam48.repository.EtudiantRepository;
import com.kfokam48.repository.PromotionRepository;
import com.kfokam48.service.EtudiantExerciceService;
import com.kfokam48.web.dto.EtudiantExerciceDto;
import com.kfokam48.web.dto.EtudiantDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * EF11 — GET /api/etudiants?promotionId= → 200 [{id, nom}] (contrat).
 * L'étudiant choisit son nom dans cette liste (Q1, H8 : pas de mot de passe).
 * promotion inconnue → 404 PROMOTION_INCONNUE. Aucune logique métier ici (B3).
 */
@RestController
public class EtudiantController {

    private final PromotionRepository promotions;
    private final EtudiantRepository etudiants;
    private final EtudiantExerciceService exercicesEtudiant;

    public EtudiantController(PromotionRepository promotions, EtudiantRepository etudiants,
                              EtudiantExerciceService exercicesEtudiant) {
        this.promotions = promotions;
        this.etudiants = etudiants;
        this.exercicesEtudiant = exercicesEtudiant;
    }

    @GetMapping("/api/etudiants/{id}/exercices")
    public List<EtudiantExerciceDto> exercices(@PathVariable Long id) {
        return exercicesEtudiant.lire(id);
    }

    @GetMapping("/api/etudiants")
    public List<EtudiantDto> lister(@RequestParam("promotionId") Long promotionId) {
        promotions.findById(promotionId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE",
                "La promotion " + promotionId + " n'existe pas."));
        return etudiants.findByPromotionIdOrderByNomAsc(promotionId).stream()
                .map(EtudiantDto::de)
                .toList();
    }
}
