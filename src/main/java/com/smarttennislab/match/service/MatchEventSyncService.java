package com.smarttennislab.match.service;

import com.smarttennislab.catalog.model.Kpi;
import com.smarttennislab.match.dto.EventSyncRequest;
import com.smarttennislab.match.dto.EventSyncResponse;
import com.smarttennislab.match.dto.MatchEventResponse;
import com.smarttennislab.match.model.Match;
import com.smarttennislab.match.model.MatchEvent;
import com.smarttennislab.match.repository.MatchEventRepository;
import com.smarttennislab.match.repository.MatchSetRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchEventSyncService {

    private final MatchEventRepository matchEventRepository;
    private final MatchSetRepository matchSetRepository;
    private final MatchService matchService;

    public MatchEventSyncService(
            MatchEventRepository matchEventRepository,
            MatchSetRepository matchSetRepository,
            MatchService matchService) {
        this.matchEventRepository = matchEventRepository;
        this.matchSetRepository = matchSetRepository;
        this.matchService = matchService;
    }

    // Idempotente por diseño: la clave es el id que generó el celular. Un evento que ya está solo
    // puede cambiar su estado de borrado, así que reenviar el mismo lote no altera los totales.
    // Se aceptan eventos aunque el partido ya esté cerrado: el lote puede llegar tarde.
    @Transactional
    public EventSyncResponse sync(UUID coachId, UUID matchId, EventSyncRequest request) {
        Match match = matchService.require(coachId, matchId);

        Set<UUID> setsDelPartido =
                matchSetRepository.findByMatchIdOrderBySetNumberAsc(matchId).stream()
                        .map(set -> set.getId())
                        .collect(Collectors.toSet());

        List<UUID> ids = request.events().stream().map(EventSyncRequest.Event::id).toList();
        Map<UUID, MatchEvent> existentes = matchEventRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(MatchEvent::getId, event -> event, (a, b) -> a, HashMap::new));

        List<EventSyncResponse.Rejected> rechazados = new ArrayList<>();
        List<MatchEvent> aGuardar = new ArrayList<>();
        int aceptados = 0;
        int ignorados = 0;

        for (EventSyncRequest.Event entrada : request.events()) {
            Optional<String> problema = validar(entrada, match, setsDelPartido, existentes.get(entrada.id()));
            if (problema.isPresent()) {
                rechazados.add(new EventSyncResponse.Rejected(entrada.id(), problema.get()));
                continue;
            }

            MatchEvent existente = existentes.get(entrada.id());
            if (existente != null) {
                if (existente.isDeleted() == entrada.deleted()) {
                    ignorados++;
                } else {
                    existente.setDeletedAt(entrada.deleted() ? Instant.now() : null);
                    aGuardar.add(existente);
                    aceptados++;
                }
                continue;
            }

            MatchEvent nuevo = new MatchEvent(
                    entrada.id(),
                    matchId,
                    entrada.setId(),
                    entrada.kpiCode(),
                    entrada.occurredAt(),
                    entrada.clientSeq());
            if (entrada.deleted()) {
                nuevo.setDeletedAt(Instant.now());
            }
            aGuardar.add(nuevo);
            aceptados++;
        }

        matchEventRepository.saveAll(aGuardar);

        return new EventSyncResponse(
                aceptados,
                ignorados,
                rechazados,
                matchEventRepository.countByMatchIdAndDeletedAtIsNull(matchId),
                Instant.now());
    }

    @Transactional(readOnly = true)
    public List<MatchEventResponse> pull(UUID coachId, UUID matchId, Instant since) {
        matchService.require(coachId, matchId);

        List<MatchEvent> eventos = since == null
                ? matchEventRepository.findByMatchIdAndDeletedAtIsNullOrderByClientSeqAsc(matchId)
                : matchEventRepository.findByMatchIdAndRecordedAtGreaterThanOrderByRecordedAtAsc(
                        matchId, since);

        return eventos.stream().map(MatchEventResponse::from).toList();
    }

    // Un evento inválido no tira abajo el lote entero: se rechaza solo ese y el resto entra. Si el
    // celular manda algo raro, el profe no pierde los otros 200 taps del partido.
    private Optional<String> validar(
            EventSyncRequest.Event entrada, Match match, Set<UUID> setsDelPartido, MatchEvent existente) {

        if (existente != null && !existente.getMatchId().equals(match.getId())) {
            return Optional.of("Ese id de evento pertenece a otro partido");
        }

        Optional<Kpi> kpi = Kpi.fromCode(entrada.kpiCode());
        if (kpi.isEmpty()) {
            return Optional.of("KPI desconocido: " + entrada.kpiCode());
        }
        if (!kpi.get().isCounter()) {
            return Optional.of(entrada.kpiCode() + " es un KPI calculado, no se toca");
        }
        if (!kpi.get().appliesTo(match.getDiscipline())) {
            return Optional.of(entrada.kpiCode() + " no aplica a " + match.getDiscipline());
        }
        if (entrada.setId() != null && !setsDelPartido.contains(entrada.setId())) {
            return Optional.of("Ese set no pertenece al partido");
        }
        return Optional.empty();
    }
}
