package com.kfokam48.web;

import com.kfokam48.domain.RelectureJpa;
import com.kfokam48.service.RelectureService;
import com.kfokam48.web.dto.RelectureDto;
import com.kfokam48.web.dto.RendreRelectureRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * EF5 — POST /api/relectures/{id} {note, commentaire} → 200 (contrat imposé).
 * L'identité du relecteur transite par le paramètre etudiantId (H9, tranché) :
 * le corps reste strictement conforme au contrat.
 * PUT /api/relectures/{id} (opération ajoutée) : correction Q10 tant que la
 * session n'est pas clôturée (Q15 après). RG13 : la réponse ne révèle jamais
 * le relecteur.
 */
@RestController
@RequestMapping("/api/relectures")
public class RelectureController {

    private final RelectureService relectureService;

    public RelectureController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    @PostMapping("/{id}")
    public RelectureDto rendre(@PathVariable Long id,
                               @RequestParam("etudiantId") Long etudiantId,
                               @Valid @RequestBody RendreRelectureRequest request) {
        RelectureJpa relecture = relectureService.rendre(id, etudiantId, request.note(), request.commentaire());
        return RelectureDto.de(relecture);
    }

    @PutMapping("/{id}")
    public RelectureDto corriger(@PathVariable Long id,
                                 @RequestParam("etudiantId") Long etudiantId,
                                 @Valid @RequestBody RendreRelectureRequest request) {
        RelectureJpa relecture = relectureService.corriger(id, etudiantId, request.note(), request.commentaire());
        return RelectureDto.de(relecture);
    }
}
