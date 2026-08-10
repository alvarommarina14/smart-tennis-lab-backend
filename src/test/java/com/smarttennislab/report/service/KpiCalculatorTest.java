package com.smarttennislab.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarttennislab.catalog.model.Discipline;
import com.smarttennislab.catalog.model.Kpi;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KpiCalculatorTest {

    private Map<Kpi, Long> contadores(Object... pares) {
        Map<Kpi, Long> mapa = new EnumMap<>(Kpi.class);
        for (int i = 0; i < pares.length; i += 2) {
            mapa.put((Kpi) pares[i], ((Number) pares[i + 1]).longValue());
        }
        return mapa;
    }

    private Map<Kpi, BigDecimal> calcular(Map<Kpi, Long> contadores, Duration duracion) {
        return KpiCalculator.calculate(contadores, duracion, Discipline.SINGLES);
    }

    @Test
    void losPuntosJugadosSonGanadosMasPerdidos() {
        Map<Kpi, BigDecimal> valores =
                calcular(contadores(Kpi.POINT_WON, 40, Kpi.POINT_LOST, 35), Duration.ofMinutes(90));

        assertThat(valores.get(Kpi.TOTAL_POINTS_WON)).isEqualByComparingTo("40");
        assertThat(valores.get(Kpi.TOTAL_POINTS_PLAYED)).isEqualByComparingTo("75");
    }

    @Test
    void elPorcentajeDePuntosGanadosSeRedondeaAUnDecimal() {
        Map<Kpi, BigDecimal> valores =
                calcular(contadores(Kpi.POINT_WON, 40, Kpi.POINT_LOST, 35), Duration.ofMinutes(90));

        assertThat(valores.get(Kpi.POINTS_WON_PCT)).isEqualByComparingTo("53.3");
    }

    @Test
    void unPartidoSinPuntosNoDividePorCeroYDa0() {
        Map<Kpi, BigDecimal> valores = calcular(contadores(), Duration.ofMinutes(5));

        assertThat(valores.get(Kpi.TOTAL_POINTS_PLAYED)).isEqualByComparingTo("0");
        assertThat(valores.get(Kpi.POINTS_WON_PCT)).isEqualByComparingTo("0.0");
    }

    @Test
    void ganarTodosLosPuntosDa100() {
        Map<Kpi, BigDecimal> valores =
                calcular(contadores(Kpi.POINT_WON, 12, Kpi.POINT_LOST, 0), Duration.ofMinutes(20));

        assertThat(valores.get(Kpi.POINTS_WON_PCT)).isEqualByComparingTo("100.0");
    }

    @Test
    void laDuracionSeExpresaEnMinutosEnteros() {
        Map<Kpi, BigDecimal> valores = calcular(contadores(), Duration.ofSeconds(5400));

        assertThat(valores.get(Kpi.MATCH_DURATION_MINUTES)).isEqualByComparingTo("90");
    }

    @Test
    void unaDuracionNegativaNoSePropagaComoNegativa() {
        Map<Kpi, BigDecimal> valores = calcular(contadores(), Duration.ofMinutes(-10));

        assertThat(valores.get(Kpi.MATCH_DURATION_MINUTES)).isEqualByComparingTo("0");
    }

    @Test
    void losContadoresQueNoSeTocaronValenCeroYNoFaltan() {
        Map<Kpi, BigDecimal> valores = calcular(contadores(Kpi.ACE, 3), Duration.ofMinutes(60));

        assertThat(valores.get(Kpi.ACE)).isEqualByComparingTo("3");
        assertThat(valores.get(Kpi.DOUBLE_FAULT)).isEqualByComparingTo("0");
        assertThat(valores.keySet()).containsAll(Kpi.counters(Discipline.SINGLES));
    }

    @Test
    void elResultadoTraeLos23KpisDeLaV1() {
        Map<Kpi, BigDecimal> valores = calcular(contadores(Kpi.ACE, 1), Duration.ofMinutes(60));

        assertThat(valores).hasSize(23);
    }

    @Test
    void unKpiCalculadoNoSeLeeDeLosContadores() {
        Map<Kpi, BigDecimal> valores = calcular(
                contadores(Kpi.TOTAL_POINTS_WON, 999, Kpi.POINT_WON, 5, Kpi.POINT_LOST, 5),
                Duration.ofMinutes(30));

        assertThat(valores.get(Kpi.TOTAL_POINTS_WON)).isEqualByComparingTo("5");
    }

    @Test
    void redondeaHaciaArribaEnElMedio() {
        assertThat(KpiCalculator.percentage(1, 8)).isEqualByComparingTo("12.5");
        assertThat(KpiCalculator.percentage(1, 3)).isEqualByComparingTo("33.3");
        assertThat(KpiCalculator.percentage(2, 3)).isEqualByComparingTo("66.7");
    }
}
