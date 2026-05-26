package com.radar.prospection.repository

import com.radar.prospection.domain.Mission
import com.radar.prospection.domain.MissionStatus
import com.radar.prospection.domain.Source
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MissionRepository : JpaRepository<Mission, Long> {
    fun findBySourceAndExternalId(source: Source, externalId: String): Mission?
    fun existsBySourceAndExternalId(source: Source, externalId: String): Boolean
    fun findByStatusOrderByFitScoreDesc(status: MissionStatus): List<Mission>
    fun findByFitScoreGreaterThanEqualOrderByFitScoreDesc(minScore: Int): List<Mission>
    fun findByFitScoreIsNull(): List<Mission>
    fun findByFavoriteTrueOrderByFitScoreDesc(): List<Mission>

    @Query("SELECT m FROM Mission m WHERE m.status = 'NEW' ORDER BY m.detectedAt DESC")
    fun findPendingAnalysis(): List<Mission>

    @Query("SELECT m FROM Mission m WHERE m.detectedAt >= :since ORDER BY m.fitScore DESC NULLS LAST")
    fun findRecentMissions(since: LocalDateTime): List<Mission>

    @Query("SELECT COUNT(m) FROM Mission m WHERE m.source = :source AND m.detectedAt >= :since")
    fun countBySourceSince(source: Source, since: LocalDateTime): Long
}
