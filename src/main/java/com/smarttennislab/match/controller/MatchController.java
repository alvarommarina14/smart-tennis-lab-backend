package com.smarttennislab.match.controller;

import com.smarttennislab.auth.model.CoachPrincipal;
import com.smarttennislab.match.dto.CreateMatchRequest;
import com.smarttennislab.match.dto.EventSyncRequest;
import com.smarttennislab.match.dto.EventSyncResponse;
import com.smarttennislab.match.dto.FinishMatchRequest;
import com.smarttennislab.match.dto.MatchEventResponse;
import com.smarttennislab.match.dto.MatchResponse;
import com.smarttennislab.match.dto.MatchSetResponse;
import com.smarttennislab.match.dto.MatchSummaryResponse;
import com.smarttennislab.match.dto.StartSetRequest;
import com.smarttennislab.match.service.MatchEventSyncService;
import com.smarttennislab.match.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
@Tag(name = "Partidos", description = "Partidos, sets y sincronización de los taps del profe")
public class MatchController {

    private final MatchService matchService;
    private final MatchEventSyncService syncService;

    public MatchController(MatchService matchService, MatchEventSyncService syncService) {
        this.matchService = matchService;
        this.syncService = syncService;
    }

    @GetMapping
    @Operation(summary = "Listar partidos, del más nuevo al más viejo")
    public List<MatchSummaryResponse> list(
            @AuthenticationPrincipal CoachPrincipal coach,
            @RequestParam(required = false) UUID playerId) {
        return matchService.list(coach.id(), playerId);
    }

    @PostMapping
    @Operation(
            summary = "Crear un partido",
            description = "El id lo genera el celular. Reenviar el mismo id devuelve el partido ya creado.")
    public ResponseEntity<MatchResponse> create(
            @AuthenticationPrincipal CoachPrincipal coach, @Valid @RequestBody CreateMatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchService.create(coach.id(), request));
    }

    @GetMapping("/{matchId}")
    @Operation(summary = "Ver un partido con sus sets")
    public MatchResponse get(
            @AuthenticationPrincipal CoachPrincipal coach, @PathVariable UUID matchId) {
        return matchService.get(coach.id(), matchId);
    }

    @PostMapping("/{matchId}/finish")
    @Operation(summary = "Cerrar un partido")
    public MatchResponse finish(
            @AuthenticationPrincipal CoachPrincipal coach,
            @PathVariable UUID matchId,
            @Valid @RequestBody FinishMatchRequest request) {
        return matchService.finish(coach.id(), matchId, request);
    }

    @PostMapping("/{matchId}/sets")
    @Operation(summary = "Abrir un set")
    public ResponseEntity<MatchSetResponse> startSet(
            @AuthenticationPrincipal CoachPrincipal coach,
            @PathVariable UUID matchId,
            @Valid @RequestBody StartSetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.startSet(coach.id(), matchId, request));
    }

    @PostMapping("/{matchId}/sets/{setId}/finish")
    @Operation(summary = "Cerrar un set")
    public MatchSetResponse finishSet(
            @AuthenticationPrincipal CoachPrincipal coach,
            @PathVariable UUID matchId,
            @PathVariable UUID setId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant finishedAt) {
        return matchService.finishSet(coach.id(), matchId, setId, finishedAt);
    }

    @PostMapping("/{matchId}/events")
    @Operation(
            summary = "Sincronizar los taps del partido",
            description = "Idempotente: reenviar el mismo lote no duplica ni altera los totales.")
    public EventSyncResponse syncEvents(
            @AuthenticationPrincipal CoachPrincipal coach,
            @PathVariable UUID matchId,
            @Valid @RequestBody EventSyncRequest request) {
        return syncService.sync(coach.id(), matchId, request);
    }

    @GetMapping("/{matchId}/events")
    @Operation(
            summary = "Traer los eventos del partido",
            description = "Con ?since= trae solo lo que llegó al servidor después de esa marca.")
    public List<MatchEventResponse> events(
            @AuthenticationPrincipal CoachPrincipal coach,
            @PathVariable UUID matchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant since) {
        return syncService.pull(coach.id(), matchId, since);
    }
}
