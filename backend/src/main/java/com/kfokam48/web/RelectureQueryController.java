package com.kfokam48.web;

import com.kfokam48.service.RelectureQueryService;
import com.kfokam48.web.dto.RelectureAffectationDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** EF5 — liste les affectations à traiter ou déjà rendues par un étudiant. */
@RestController
@RequestMapping("/api/relectures")
public class RelectureQueryController {

    private final RelectureQueryService service;

    public RelectureQueryController(RelectureQueryService service) {
        this.service = service;
    }

    @GetMapping
    public List<RelectureAffectationDto> lister(
            @RequestParam("etudiantId") Long etudiantId,
            @RequestParam(name = "rendue", defaultValue = "false") boolean rendue) {
        return service.lister(etudiantId, rendue);
    }
}
