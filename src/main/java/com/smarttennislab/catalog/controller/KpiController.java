package com.smarttennislab.catalog.controller;

import com.smarttennislab.catalog.dto.KpiCatalogResponse;
import com.smarttennislab.catalog.model.Discipline;
import com.smarttennislab.catalog.service.KpiCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kpis")
@Tag(name = "Catálogo", description = "KPIs disponibles para capturar y reportar")
public class KpiController {

    private final KpiCatalogService kpiCatalogService;

    public KpiController(KpiCatalogService kpiCatalogService) {
        this.kpiCatalogService = kpiCatalogService;
    }

    @GetMapping
    @Operation(
            summary = "Catálogo de KPIs agrupado por categoría",
            description = "La app arma la pantalla de captura con esta respuesta: no hardcodea "
                    + "ningún KPI, así agregar KPIs nuevos no requiere publicar una versión nueva.")
    public KpiCatalogResponse catalog(
            @RequestParam(defaultValue = "SINGLES") Discipline discipline) {
        return kpiCatalogService.catalogFor(discipline);
    }
}
