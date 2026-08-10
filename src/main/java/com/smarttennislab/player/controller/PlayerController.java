package com.smarttennislab.player.controller;

import com.smarttennislab.auth.model.CoachPrincipal;
import com.smarttennislab.player.dto.PlayerRequest;
import com.smarttennislab.player.dto.PlayerResponse;
import com.smarttennislab.player.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
@Tag(name = "Alumnos", description = "Los alumnos del profe logueado")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    @Operation(summary = "Listar alumnos")
    public List<PlayerResponse> list(
            @AuthenticationPrincipal CoachPrincipal coach,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return playerService.list(coach.id(), includeArchived);
    }

    @GetMapping("/{playerId}")
    @Operation(summary = "Ver un alumno")
    public PlayerResponse get(
            @AuthenticationPrincipal CoachPrincipal coach, @PathVariable UUID playerId) {
        return playerService.get(coach.id(), playerId);
    }

    @PostMapping
    @Operation(summary = "Crear un alumno")
    public ResponseEntity<PlayerResponse> create(
            @AuthenticationPrincipal CoachPrincipal coach, @Valid @RequestBody PlayerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerService.create(coach.id(), request));
    }

    @PutMapping("/{playerId}")
    @Operation(summary = "Editar un alumno")
    public PlayerResponse update(
            @AuthenticationPrincipal CoachPrincipal coach,
            @PathVariable UUID playerId,
            @Valid @RequestBody PlayerRequest request) {
        return playerService.update(coach.id(), playerId, request);
    }

    @DeleteMapping("/{playerId}")
    @Operation(
            summary = "Archivar un alumno",
            description = "No se borra: los partidos que jugó siguen existiendo.")
    public ResponseEntity<Void> archive(
            @AuthenticationPrincipal CoachPrincipal coach, @PathVariable UUID playerId) {
        playerService.archive(coach.id(), playerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{playerId}/restore")
    @Operation(summary = "Desarchivar un alumno")
    public ResponseEntity<Void> restore(
            @AuthenticationPrincipal CoachPrincipal coach, @PathVariable UUID playerId) {
        playerService.restore(coach.id(), playerId);
        return ResponseEntity.noContent().build();
    }
}
