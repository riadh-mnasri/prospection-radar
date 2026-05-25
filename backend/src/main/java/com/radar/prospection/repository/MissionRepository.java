package com.radar.prospection.repository;

import com.radar.prospection.domain.Mission;
import com.radar.prospection.domain.MissionStatus;
import com.radar.prospection.domain.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    Optional<Mission> findBySourceAndExternalId(Source source, String externalId);

    boolean existsBySourceAndExternalId(Source source, String externalId);

    List<Mission> findByStatusOrderByFitScoreDesc(MissionStatus status);

    List<Mission> findByFitScoreGreaterThanEqualOrderByFitScoreDesc(int minScore);

    @Query("SELECT m FROM Mission m WHERE m.status = 'NEW' ORDER BY m.detectedAt DESC")
    List<Mission> findPendingAnalysis();

    @Query("SELECT m FROM Mission m WHERE m.detectedAt >= :since ORDER BY m.fitScore DESC NULLS LAST")
    List<Mission> findRecentMissions(LocalDateTime since);

    @Query("SELECT COUNT(m) FROM Mission m WHERE m.source = :source AND m.detectedAt >= :since")
    long countBySourceSince(Source source, LocalDateTime since);

    List<Mission> findByFitScoreIsNull();

    List<Mission> findByFavoriteTrueOrderByFitScoreDesc();
}
