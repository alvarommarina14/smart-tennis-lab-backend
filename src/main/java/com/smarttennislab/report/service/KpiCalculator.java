package com.smarttennislab.report.service;

import com.smarttennislab.catalog.model.Discipline;
import com.smarttennislab.catalog.model.Kpi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

// Cálculo puro: entra cuántas veces se tocó cada botón y cuánto duró, sale el valor de los 23 KPIs.
// No sabe nada de JPA ni de HTTP, así que se testea sin levantar nada.
public final class KpiCalculator {

    private KpiCalculator() {}

    public static Map<Kpi, BigDecimal> calculate(
            Map<Kpi, Long> counters, Duration duration, Discipline discipline) {

        Map<Kpi, BigDecimal> valores = new EnumMap<>(Kpi.class);

        for (Kpi kpi : Kpi.counters(discipline)) {
            valores.put(kpi, BigDecimal.valueOf(count(counters, kpi)));
        }

        long ganados = count(counters, Kpi.POINT_WON);
        long perdidos = count(counters, Kpi.POINT_LOST);
        long jugados = ganados + perdidos;

        valores.put(Kpi.TOTAL_POINTS_WON, BigDecimal.valueOf(ganados));
        valores.put(Kpi.TOTAL_POINTS_PLAYED, BigDecimal.valueOf(jugados));
        valores.put(Kpi.MATCH_DURATION_MINUTES, BigDecimal.valueOf(Math.max(0, duration.toMinutes())));
        valores.put(Kpi.POINTS_WON_PCT, percentage(ganados, jugados));

        return valores;
    }

    // Sin puntos jugados el porcentaje es 0, no una división por cero ni un null que la app tenga
    // que contemplar.
    static BigDecimal percentage(long parte, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(parte)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private static long count(Map<Kpi, Long> counters, Kpi kpi) {
        return counters.getOrDefault(kpi, 0L);
    }
}
