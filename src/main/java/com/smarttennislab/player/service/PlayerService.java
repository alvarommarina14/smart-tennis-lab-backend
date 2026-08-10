package com.smarttennislab.player.service;

import com.smarttennislab.player.dto.PlayerRequest;
import com.smarttennislab.player.dto.PlayerResponse;
import com.smarttennislab.player.model.Player;
import com.smarttennislab.player.repository.PlayerRepository;
import com.smarttennislab.shared.ApiException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public List<PlayerResponse> list(UUID coachId, boolean includeArchived) {
        List<Player> players = includeArchived
                ? playerRepository.findByCoachIdOrderByLastNameAscFirstNameAsc(coachId)
                : playerRepository.findByCoachIdAndArchivedAtIsNullOrderByLastNameAscFirstNameAsc(coachId);
        return players.stream().map(PlayerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlayerResponse get(UUID coachId, UUID playerId) {
        return PlayerResponse.from(require(coachId, playerId));
    }

    @Transactional
    public PlayerResponse create(UUID coachId, PlayerRequest request) {
        Player player = new Player(coachId, request.firstName().trim(), request.lastName().trim());
        apply(player, request);
        return PlayerResponse.from(playerRepository.save(player));
    }

    @Transactional
    public PlayerResponse update(UUID coachId, UUID playerId, PlayerRequest request) {
        Player player = require(coachId, playerId);
        player.setFirstName(request.firstName().trim());
        player.setLastName(request.lastName().trim());
        apply(player, request);
        return PlayerResponse.from(playerRepository.save(player));
    }

    // No se borra: los partidos jugados siguen apuntando al alumno. Archivar lo saca de la lista.
    @Transactional
    public void archive(UUID coachId, UUID playerId) {
        require(coachId, playerId).archive();
    }

    @Transactional
    public void restore(UUID coachId, UUID playerId) {
        require(coachId, playerId).restore();
    }

    // Para las listas de partidos: un solo viaje a la base en vez de un SELECT por partido.
    @Transactional(readOnly = true)
    public Map<UUID, String> namesByIdFor(UUID coachId) {
        return playerRepository.findByCoachIdOrderByLastNameAscFirstNameAsc(coachId).stream()
                .collect(Collectors.toMap(Player::getId, PlayerService::displayName));
    }

    public static String displayName(Player player) {
        return player.getFirstName() + " " + player.getLastName();
    }

    // Un alumno de otro profe se responde como inexistente, no como prohibido: así la API no
    // confirma que ese id existe.
    public Player require(UUID coachId, UUID playerId) {
        return playerRepository
                .findByIdAndCoachId(playerId, coachId)
                .orElseThrow(() -> ApiException.notFound("Ese alumno no existe"));
    }

    private void apply(Player player, PlayerRequest request) {
        player.setBirthDate(request.birthDate());
        player.setDominantHand(request.dominantHand());
        player.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
    }
}
