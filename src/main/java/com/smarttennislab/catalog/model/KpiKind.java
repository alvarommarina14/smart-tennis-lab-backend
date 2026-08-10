package com.smarttennislab.catalog.model;

public enum KpiKind {

    // Botón que el profe toca en vivo. Cada tap es una fila en match_events.
    COUNTER,

    // No se toca: lo calcula MatchReportService.
    DERIVED
}
