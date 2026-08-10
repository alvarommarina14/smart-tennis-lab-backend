package com.smarttennislab.match.repository;

import com.smarttennislab.match.model.MatchEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchEventRepository extends JpaRepository<MatchEvent, UUID> {

    // Los totales del reporte salen de acá: un GROUP BY sobre el índice parcial de eventos vivos.
    @Query("select e.kpiCode as kpiCode, count(e) as total from MatchEvent e "
            + "where e.matchId = :matchId and e.deletedAt is null "
            + "group by e.kpiCode")
    List<KpiCount> countByKpi(@Param("matchId") UUID matchId);

    @Query("select e.setId as setId, e.kpiCode as kpiCode, count(e) as total from MatchEvent e "
            + "where e.matchId = :matchId and e.deletedAt is null and e.setId is not null "
            + "group by e.setId, e.kpiCode")
    List<KpiCountBySet> countByKpiAndSet(@Param("matchId") UUID matchId);

    List<MatchEvent> findByMatchIdAndDeletedAtIsNullOrderByClientSeqAsc(UUID matchId);

    // Pull incremental: la app pide solo lo que llegó al servidor después de su última sync.
    List<MatchEvent> findByMatchIdAndRecordedAtGreaterThanOrderByRecordedAtAsc(
            UUID matchId, Instant since);

    long countByMatchIdAndDeletedAtIsNull(UUID matchId);
}
