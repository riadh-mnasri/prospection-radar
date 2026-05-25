package com.radar.prospection.repository;

import com.radar.prospection.domain.Signal;
import com.radar.prospection.domain.SignalStatus;
import com.radar.prospection.domain.SignalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SignalRepository extends JpaRepository<Signal, Long> {

    List<Signal> findByStatusOrderByOpportunityScoreDesc(SignalStatus status);

    List<Signal> findByTypeAndDetectedAtAfter(SignalType type, LocalDateTime since);

    @Query("SELECT s FROM Signal s WHERE s.opportunityScore >= :minScore ORDER BY s.detectedAt DESC")
    List<Signal> findHighValueSignals(int minScore);

    boolean existsByCompanyAndTypeAndDetectedAtAfter(String company, SignalType type, LocalDateTime since);
}
