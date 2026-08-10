package com.smarttennislab.match.model;

import com.smarttennislab.catalog.model.Discipline;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// El id lo genera el celular: el profe tiene que poder empezar un partido sin señal.
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id private UUID id;

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "opponent_name")
    private String opponentName;

    @Column(name = "tournament")
    private String tournament;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface")
    private Surface surface;

    @Enumerated(EnumType.STRING)
    @Column(name = "discipline", nullable = false)
    private Discipline discipline = Discipline.SINGLES;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MatchStatus status = MatchStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Match(UUID id, UUID coachId, UUID playerId, Instant startedAt) {
        this.id = id;
        this.coachId = coachId;
        this.playerId = playerId;
        this.startedAt = startedAt;
    }

    public boolean isOpen() {
        return status == MatchStatus.IN_PROGRESS;
    }

    public void finish(MatchStatus finalStatus, Instant finishedAt) {
        this.status = finalStatus;
        this.finishedAt = finishedAt;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
