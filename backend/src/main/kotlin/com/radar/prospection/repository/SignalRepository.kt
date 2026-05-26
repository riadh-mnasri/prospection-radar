package com.radar.prospection.repository

import com.radar.prospection.domain.Signal
import com.radar.prospection.domain.SignalStatus
import com.radar.prospection.domain.SignalType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SignalRepository : JpaRepository<Signal, Long> {
    fun findByStatusOrderByOpportunityScoreDesc(status: SignalStatus): List<Signal>
    fun findByTypeAndDetectedAtAfter(type: SignalType, since: LocalDateTime): List<Signal>
    fun existsByCompanyAndTypeAndDetectedAtAfter(company: String, type: SignalType, since: LocalDateTime): Boolean

    @Query("SELECT s FROM Signal s WHERE s.opportunityScore >= :minScore ORDER BY s.detectedAt DESC")
    fun findHighValueSignals(minScore: Int): List<Signal>
}
