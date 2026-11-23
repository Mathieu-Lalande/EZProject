package com.ezproject.portfolio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Accessible à tous : le périmètre est restreint aux projets visibles par l'utilisateur. */
@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping
    public PortfolioDto overview(@RequestParam(required = false) Long teamId) {
        return service.overview(teamId);
    }
}
