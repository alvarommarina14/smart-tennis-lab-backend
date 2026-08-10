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

// Un tap del profe = una fila. El id lo genera el celular y es la clave de idempotencia: reenviar
// el mismo lote no duplica nada. Deshacer no borra, marca deletedAt.
@Entity
@Table(name = "match_events")
@Getter
@Setter
@NoArgsConstructor
public class MatchEvent {

    @Id private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "set_id")
    private UUID setId;

    @Column(name = "kpi_code", nullable = false)
    private String kpiCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "client_seq", nullable = false)
    private long clientSeq;

    public MatchEvent(
            UUID id, UUID matchId, UUID setId, String kpiCode, Instant occurredAt, long clientSeq) {
        this.id = id;
        this.matchId = matchId;
        this.setId = setId;
        this.kpiCode = kpiCode;
        this.occurredAt = occurredAt;
        this.clientSeq = clientSeq;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
