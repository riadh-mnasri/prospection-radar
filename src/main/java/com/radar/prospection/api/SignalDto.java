package com.radar.prospection.api;

import com.radar.prospection.domain.Signal;
import com.radar.prospection.domain.SignalType;

import java.time.LocalDateTime;

public record SignalDto(
    Long id,
    SignalType type,
    String company,
    String title,
    String description,
    String fundingAmount,
    String personName,
    String newRole,
    Integer opportunityScore,
    String opportunityReason,
    String suggestedAction,
    String url,
    LocalDateTime detectedAt
) {
    public static SignalDto from(Signal s) {
        return new SignalDto(
            s.getId(), s.getType(), s.getCompany(), s.getTitle(),
            s.getDescription(), s.getFundingAmount(), s.getPersonName(),
            s.getNewRole(), s.getOpportunityScore(), s.getOpportunityReason(),
            s.getSuggestedAction(), s.getUrl(), s.getDetectedAt()
        );
    }
}
