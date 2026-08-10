package com.smarttennislab.match.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Solo separa el partido en tramos: no hay reglamento de tenis acá, el profe decide cuándo cierra
// un set.
@Entity
@Table(name = "match_sets")
@Getter
@Setter
@NoArgsConstructor
public class MatchSet {

    @Id private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "set_number", nullable = false)
    private short setNumber;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public MatchSet(UUID id, UUID matchId, short setNumber, Instant startedAt) {
        this.id = id;
        this.matchId = matchId;
        this.setNumber = setNumber;
        this.startedAt = startedAt;
    }
}
