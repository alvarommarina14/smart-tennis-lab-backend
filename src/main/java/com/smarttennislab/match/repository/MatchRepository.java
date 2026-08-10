package com.smarttennislab.match.repository;

import com.smarttennislab.match.model.Match;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    Optional<Match> findByIdAndCoachId(UUID id, UUID coachId);

    List<Match> findByCoachIdOrderByStartedAtDesc(UUID coachId);

    List<Match> findByCoachIdAndPlayerIdOrderByStartedAtDesc(UUID coachId, UUID playerId);

    boolean existsByIdAndCoachId(UUID id, UUID coachId);
}
