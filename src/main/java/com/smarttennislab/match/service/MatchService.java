package com.smarttennislab.match.service;

import com.smarttennislab.catalog.model.Discipline;
import com.smarttennislab.match.dto.CreateMatchRequest;
import com.smarttennislab.match.dto.FinishMatchRequest;
import com.smarttennislab.match.dto.MatchResponse;
import com.smarttennislab.match.dto.MatchSetResponse;
import com.smarttennislab.match.dto.MatchSummaryResponse;
import com.smarttennislab.match.dto.StartSetRequest;
import com.smarttennislab.match.model.Match;
import com.smarttennislab.match.model.MatchFormat;
import com.smarttennislab.match.model.MatchSet;
import com.smarttennislab.match.model.MatchStatus;
import com.smarttennislab.match.repository.MatchEventRepository;
import com.smarttennislab.match.repository.MatchRepository;
import com.smarttennislab.match.repository.MatchSetRepository;
import com.smarttennislab.player.model.Player;
import com.smarttennislab.player.service.PlayerService;
import com.smarttennislab.shared.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchSetRepository matchSetRepository;
    private final MatchEventRepository matchEventRepository;
    private final PlayerService playerService;

    public MatchService(
            MatchRepository matchRepository,
            MatchSetRepository matchSetRepository,
            MatchEventRepository matchEventRepository,
            PlayerService playerService) {
        this.matchRepository = matchRepository;
        this.matchSetRepository = matchSetRepository;
        this.matchEventRepository = matchEventRepository;
        this.playerService = playerService;
    }

    // Reenviar la creación con el mismo id no falla ni duplica: devuelve el partido que ya está.
    // Es lo que hace que el sync después de estar sin señal sea seguro de reintentar.
    @Transactional
    public MatchResponse create(UUID coachId, CreateMatchRequest request) {
        Player player = playerService.require(coachId, request.playerId());

        Optional<Match> existente = matchRepository.findById(request.id());
        if (existente.isPresent()) {
            Match match = existente.get();
            if (!match.getCoachId().equals(coachId)) {
                throw ApiException.conflict("Ese id de partido ya está usado");
            }
            return detail(match, PlayerService.displayName(player));
        }

        Match match = new Match(request.id(), coachId, player.getId(), request.startedAt());
        match.setOpponentName(blankToNull(request.opponentName()));
        match.setTournament(blankToNull(request.tournament()));
        match.setSurface(request.surface());
        match.setDiscipline(request.discipline() == null ? Discipline.SINGLES : request.discipline());
        match.setFormat(request.format() == null ? MatchFormat.BEST_OF_3_SETS : request.format());
        match.setNotes(blankToNull(request.notes()));

        return detail(matchRepository.save(match), PlayerService.displayName(player));
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> list(UUID coachId, UUID playerId) {
        List<Match> matches = playerId == null
                ? matchRepository.findByCoachIdOrderByStartedAtDesc(coachId)
                : matchRepository.findByCoachIdAndPlayerIdOrderByStartedAtDesc(coachId, playerId);

        Map<UUID, String> names = playerService.namesByIdFor(coachId);
        return matches.stream()
                .map(match -> MatchSummaryResponse.from(match, names.get(match.getPlayerId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse get(UUID coachId, UUID matchId) {
        Match match = require(coachId, matchId);
        return detail(match, playerService.namesByIdFor(coachId).get(match.getPlayerId()));
    }

    @Transactional
    public MatchResponse finish(UUID coachId, UUID matchId, FinishMatchRequest request) {
        Match match = require(coachId, matchId);

        if (request.status() == MatchStatus.IN_PROGRESS) {
            throw ApiException.badRequest("Para terminar un partido el estado no puede ser IN_PROGRESS");
        }
        if (request.finishedAt().isBefore(match.getStartedAt())) {
            throw ApiException.badRequest("El partido no puede terminar antes de empezar");
        }

        match.finish(request.status(), request.finishedAt());
        if (request.notes() != null) {
            match.setNotes(blankToNull(request.notes()));
        }
        matchSetRepository.findByMatchIdOrderBySetNumberAsc(matchId).stream()
                .filter(set -> set.getFinishedAt() == null)
                .forEach(set -> set.setFinishedAt(request.finishedAt()));

        return detail(match, playerService.namesByIdFor(coachId).get(match.getPlayerId()));
    }

    @Transactional
    public MatchSetResponse startSet(UUID coachId, UUID matchId, StartSetRequest request) {
        Match match = require(coachId, matchId);
        if (!match.isOpen()) {
            throw ApiException.badRequest("El partido ya está cerrado");
        }

        Optional<MatchSet> yaExiste = matchSetRepository.findById(request.id());
        if (yaExiste.isPresent()) {
            requireBelongsToMatch(yaExiste.get(), matchId);
            return MatchSetResponse.from(yaExiste.get());
        }
        if (matchSetRepository.findByMatchIdAndSetNumber(matchId, request.setNumber()).isPresent()) {
            throw ApiException.conflict("Ese set ya fue creado");
        }

        MatchSet set = new MatchSet(request.id(), matchId, request.setNumber(), request.startedAt());
        return MatchSetResponse.from(matchSetRepository.save(set));
    }

    @Transactional
    public MatchSetResponse finishSet(UUID coachId, UUID matchId, UUID setId, Instant finishedAt) {
        require(coachId, matchId);
        MatchSet set = matchSetRepository
                .findById(setId)
                .orElseThrow(() -> ApiException.notFound("Ese set no existe"));
        requireBelongsToMatch(set, matchId);

        if (finishedAt.isBefore(set.getStartedAt())) {
            throw ApiException.badRequest("El set no puede terminar antes de empezar");
        }
        set.setFinishedAt(finishedAt);
        return MatchSetResponse.from(set);
    }

    public Match require(UUID coachId, UUID matchId) {
        return matchRepository
                .findByIdAndCoachId(matchId, coachId)
                .orElseThrow(() -> ApiException.notFound("Ese partido no existe"));
    }

    private void requireBelongsToMatch(MatchSet set, UUID matchId) {
        if (!set.getMatchId().equals(matchId)) {
            throw ApiException.notFound("Ese set no existe");
        }
    }

    private MatchResponse detail(Match match, String playerName) {
        List<MatchSetResponse> sets =
                matchSetRepository.findByMatchIdOrderBySetNumberAsc(match.getId()).stream()
                        .map(MatchSetResponse::from)
                        .toList();
        long eventCount = matchEventRepository.countByMatchIdAndDeletedAtIsNull(match.getId());
        return MatchResponse.from(match, playerName, eventCount, sets);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
