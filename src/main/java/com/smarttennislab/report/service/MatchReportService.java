package com.smarttennislab.report.service;

import com.smarttennislab.catalog.model.Kpi;
import com.smarttennislab.catalog.model.KpiCategory;
import com.smarttennislab.match.model.Match;
import com.smarttennislab.match.model.MatchSet;
import com.smarttennislab.match.repository.KpiCount;
import com.smarttennislab.match.repository.KpiCountBySet;
import com.smarttennislab.match.repository.MatchEventRepository;
import com.smarttennislab.match.repository.MatchSetRepository;
import com.smarttennislab.match.service.MatchService;
import com.smarttennislab.player.service.PlayerService;
import com.smarttennislab.report.dto.KpiValueResponse;
import com.smarttennislab.report.dto.MatchReportResponse;
import com.smarttennislab.report.dto.ReportCategoryResponse;
import com.smarttennislab.report.dto.SetReportResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchReportService {

    private final MatchService matchService;
    private final MatchSetRepository matchSetRepository;
    private final MatchEventRepository matchEventRepository;
    private final PlayerService playerService;

    public MatchReportService(
            MatchService matchService,
            MatchSetRepository matchSetRepository,
            MatchEventRepository matchEventRepository,
            PlayerService playerService) {
        this.matchService = matchService;
        this.matchSetRepository = matchSetRepository;
        this.matchEventRepository = matchEventRepository;
        this.playerService = playerService;
    }

    @Transactional(readOnly = true)
    public MatchReportResponse report(UUID coachId, UUID matchId) {
        Match match = matchService.require(coachId, matchId);

        Map<Kpi, Long> totales = toCounters(matchEventRepository.countByKpi(matchId));
        Duration duracion = durationOf(match);
        Map<Kpi, BigDecimal> valores = KpiCalculator.calculate(totales, duracion, match.getDiscipline());

        String playerName = playerService.namesByIdFor(coachId).get(match.getPlayerId());

        return new MatchReportResponse(
                match.getId(),
                playerName,
                match.getOpponentName(),
                match.getTournament(),
                match.getStatus(),
                match.getStartedAt(),
                match.getFinishedAt(),
                Math.max(0, duracion.toMinutes()),
                totales.values().stream().mapToLong(Long::longValue).sum(),
                agrupar(valores, match),
                setsReport(match, matchId));
    }

    private List<ReportCategoryResponse> agrupar(Map<Kpi, BigDecimal> valores, Match match) {
        List<ReportCategoryResponse> categorias = new ArrayList<>();

        for (KpiCategory categoria : KpiCategory.values()) {
            List<KpiValueResponse> kpis = Kpi.forDiscipline(match.getDiscipline()).stream()
                    .filter(kpi -> kpi.getCategory() == categoria)
                    .map(kpi -> KpiValueResponse.of(kpi, valores.getOrDefault(kpi, BigDecimal.ZERO)))
                    .toList();
            if (!kpis.isEmpty()) {
                categorias.add(new ReportCategoryResponse(categoria.name(), categoria.getLabel(), kpis));
            }
        }
        return categorias;
    }

    private List<SetReportResponse> setsReport(Match match, UUID matchId) {
        List<MatchSet> sets = matchSetRepository.findByMatchIdOrderBySetNumberAsc(matchId);
        if (sets.isEmpty()) {
            return List.of();
        }

        Map<UUID, Map<Kpi, Long>> porSet = new HashMap<>();
        for (KpiCountBySet fila : matchEventRepository.countByKpiAndSet(matchId)) {
            Kpi.fromCode(fila.getKpiCode())
                    .ifPresent(kpi -> porSet
                            .computeIfAbsent(fila.getSetId(), id -> new EnumMap<>(Kpi.class))
                            .put(kpi, fila.getTotal()));
        }

        return sets.stream()
                .map(set -> {
                    Map<Kpi, Long> contadores = porSet.getOrDefault(set.getId(), Map.of());
                    Map<Kpi, BigDecimal> valores = KpiCalculator.calculate(
                            contadores, durationOf(set), match.getDiscipline());
                    List<KpiValueResponse> kpis = Kpi.forDiscipline(match.getDiscipline()).stream()
                            .map(kpi -> KpiValueResponse.of(kpi, valores.getOrDefault(kpi, BigDecimal.ZERO)))
                            .toList();
                    return new SetReportResponse(
                            set.getId(), set.getSetNumber(), set.getStartedAt(), set.getFinishedAt(), kpis);
                })
                .toList();
    }

    // Un partido en curso se mide contra el reloj de ahora: el profe quiere ver la duración mientras
    // el partido pasa, no solo al final.
    private static Duration durationOf(Match match) {
        Instant fin = match.getFinishedAt() == null ? Instant.now() : match.getFinishedAt();
        return Duration.between(match.getStartedAt(), fin);
    }

    private static Duration durationOf(MatchSet set) {
        Instant fin = set.getFinishedAt() == null ? Instant.now() : set.getFinishedAt();
        return Duration.between(set.getStartedAt(), fin);
    }

    private static Map<Kpi, Long> toCounters(List<KpiCount> filas) {
        Map<Kpi, Long> contadores = new EnumMap<>(Kpi.class);
        for (KpiCount fila : filas) {
            Kpi.fromCode(fila.getKpiCode()).ifPresent(kpi -> contadores.put(kpi, fila.getTotal()));
        }
        return contadores;
    }
}
