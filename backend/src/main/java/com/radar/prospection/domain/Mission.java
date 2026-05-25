package com.radar.prospection.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "missions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source", "external_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Source source;

    @Column(name = "external_id")
    private String externalId;

    private String title;

    @Column(length = 5000)
    private String description;

    private String company;
    private String location;
    private Boolean remote;
    private Integer tjmMin;
    private Integer tjmMax;
    private String duration;
    private String startDate;

    @ElementCollection
    @CollectionTable(name = "mission_skills", joinColumns = @JoinColumn(name = "mission_id"))
    @Column(name = "skill")
    private List<String> skills;

    private String url;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MissionStatus status = MissionStatus.NEW;

    // Analyse Claude
    @Column(name = "fit_score")
    private Integer fitScore;

    @Column(name = "fit_summary", length = 2000)
    private String fitSummary;

    @Column(name = "decision_maker_hint", length = 500)
    private String decisionMakerHint;

    @Column(name = "detected_at")
    @Builder.Default
    private LocalDateTime detectedAt = LocalDateTime.now();

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "favorite")
    @Builder.Default
    private Boolean favorite = false;
}
