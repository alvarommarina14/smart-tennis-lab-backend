package com.smarttennislab.match.repository;

import com.smarttennislab.match.model.MatchSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSetRepository extends JpaRepository<MatchSet, UUID> {

    List<MatchSet> findByMatchIdOrderBySetNumberAsc(UUID matchId);

    Optional<MatchSet> findByMatchIdAndSetNumber(UUID matchId, short setNumber);
}
