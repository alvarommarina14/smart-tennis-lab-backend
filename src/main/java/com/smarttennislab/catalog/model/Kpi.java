package com.smarttennislab.catalog.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Catálogo cerrado de KPIs (lista base singles). La app no replica esta lista: la pide por
// GET /api/v1/kpis, así agregar KPIs no obliga a publicar una versión nueva en la store.
//
// Todos los KPIs de la lista base aplican también a dobles. Cuando se definan los KPIs propios de
// dobles se agregan acá con appliesTo = DOUBLES solamente.
//
// Fuera del alcance de v1: "% de juegos ganados" y "% break points convertidos y salvados". No son
// calculables sin eventos base que la lista no incluye.
public enum Kpi {

    // --- A. Eficiencia general (calculados, no se tocan) -------------------------------------

    TOTAL_POINTS_WON("Total de puntos ganados", KpiCategory.GENERAL_EFFICIENCY, KpiKind.DERIVED, KpiUnit.COUNT),
    TOTAL_POINTS_PLAYED("Total de puntos jugados", KpiCategory.GENERAL_EFFICIENCY, KpiKind.DERIVED, KpiUnit.COUNT),
    MATCH_DURATION_MINUTES("Duración del partido", KpiCategory.GENERAL_EFFICIENCY, KpiKind.DERIVED, KpiUnit.MINUTES),
    POINTS_WON_PCT("% de puntos ganados", KpiCategory.GENERAL_EFFICIENCY, KpiKind.DERIVED, KpiUnit.PERCENTAGE),

    // --- B. Saque ----------------------------------------------------------------------------

    FIRST_SERVE_IN("Primer saque IN", KpiCategory.SERVE),
    FIRST_SERVE_OUT("Primer saque OUT", KpiCategory.SERVE),
    SECOND_SERVE_IN("Segundo saque IN", KpiCategory.SERVE),
    ACE("Ace", KpiCategory.SERVE),
    DOUBLE_FAULT("Doble falta", KpiCategory.SERVE),
    SERVE_ERROR_OUT("Error de saque – OUT", KpiCategory.SERVE),
    SERVE_ERROR_NET("Error de saque – NET", KpiCategory.SERVE),

    // --- C. Devolución -----------------------------------------------------------------------

    RETURN_IN_PLAY("Devolución en juego", KpiCategory.RETURN),
    RETURN_ERROR_OUT("Error de devolución – OUT", KpiCategory.RETURN),
    RETURN_ERROR_NET("Error de devolución – NET", KpiCategory.RETURN),
    POINT_WON_RETURNING_FIRST_SERVE("Punto ganado devolviendo 1er saque", KpiCategory.RETURN),
    POINT_WON_RETURNING_SECOND_SERVE("Punto ganado devolviendo 2do saque", KpiCategory.RETURN),

    // --- D. Definición del punto -------------------------------------------------------------

    WINNER("Winner", KpiCategory.POINT_DEFINITION),
    UNFORCED_ERROR("Error no forzado", KpiCategory.POINT_DEFINITION),

    // --- E. Desarrollo del punto -------------------------------------------------------------

    RALLY_1_4("Rally 1–4 golpes", KpiCategory.RALLY),
    RALLY_5_8("Rally 5–8 golpes", KpiCategory.RALLY),
    RALLY_9_PLUS("Rally 9+ golpes", KpiCategory.RALLY),

    // --- G. Resultado del punto --------------------------------------------------------------

    POINT_WON("Punto ganado", KpiCategory.POINT_OUTCOME),
    POINT_LOST("Punto perdido", KpiCategory.POINT_OUTCOME);

    private final String label;
    private final KpiCategory category;
    private final KpiKind kind;
    private final KpiUnit unit;
    private final Set<Discipline> appliesTo;

    // Atajo para el caso habitual: un contador que se toca, en singles y en dobles.
    Kpi(String label, KpiCategory category) {
        this(label, category, KpiKind.COUNTER, KpiUnit.COUNT);
    }

    Kpi(String label, KpiCategory category, KpiKind kind, KpiUnit unit) {
        this(label, category, kind, unit, EnumSet.allOf(Discipline.class));
    }

    Kpi(String label, KpiCategory category, KpiKind kind, KpiUnit unit, Set<Discipline> appliesTo) {
        this.label = label;
        this.category = category;
        this.kind = kind;
        this.unit = unit;
        this.appliesTo = Collections.unmodifiableSet(EnumSet.copyOf(appliesTo));
    }

    public String getLabel() {
        return label;
    }

    public KpiCategory getCategory() {
        return category;
    }

    public KpiKind getKind() {
        return kind;
    }

    public KpiUnit getUnit() {
        return unit;
    }

    public Set<Discipline> getAppliesTo() {
        return appliesTo;
    }

    public boolean isCounter() {
        return kind == KpiKind.COUNTER;
    }

    public boolean appliesTo(Discipline discipline) {
        return appliesTo.contains(discipline);
    }

    // KPIs que la app puede mandar como evento. Todo lo demás se rechaza en el sync.
    public static List<Kpi> counters(Discipline discipline) {
        return Arrays.stream(values())
                .filter(Kpi::isCounter)
                .filter(kpi -> kpi.appliesTo(discipline))
                .toList();
    }

    public static List<Kpi> forDiscipline(Discipline discipline) {
        return Arrays.stream(values())
                .filter(kpi -> kpi.appliesTo(discipline))
                .toList();
    }

    public static Optional<Kpi> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(kpi -> kpi.name().equals(code))
                .findFirst();
    }
}
