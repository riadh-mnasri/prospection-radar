package com.radar.prospection.api;

import com.radar.prospection.domain.Mission;
import com.radar.prospection.domain.MissionStatus;
import com.radar.prospection.domain.Source;

import java.time.LocalDateTime;
import java.util.List;

public record MissionDto(
    Long id,
    Source source,
    String title,
    String company,
    String location,
    Boolean remote,
    Integer tjmMin,
    Integer tjmMax,
    String duration,
    List<String> skills,
    String url,
    MissionStatus status,
    Integer fitScore,
    String fitSummary,
    String decisionMakerHint,
    LocalDateTime detectedAt
) {
    public static MissionDto from(Mission m) {
        return new MissionDto(
            m.getId(), m.getSource(), m.getTitle(), m.getCompany(),
            m.getLocation(), m.getRemote(), m.getTjmMin(), m.getTjmMax(),
            m.getDuration(), m.getSkills(), m.getUrl(), m.getStatus(),
            m.getFitScore(), m.getFitSummary(), m.getDecisionMakerHint(),
            m.getDetectedAt()
        );
    }
}
