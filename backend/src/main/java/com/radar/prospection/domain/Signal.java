package com.radar.prospection.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "signals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Signal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SignalType type;

    private String company;
    private String title;

    @Column(length = 3000)
    private String description;

    // Pour les levées de fonds
    private String fundingAmount;
    private String fundingRound;

    // Pour les nominations
    private String personName;
    private String newRole;

    // Score d'opportunité calculé par Claude
    @Column(name = "opportunity_score")
    private Integer opportunityScore;

    @Column(name = "opportunity_reason", length = 1000)
    private String opportunityReason;

    @Column(name = "suggested_action", length = 500)
    private String suggestedAction;

    private String url;
    private String sourceWebsite;

    @Column(name = "detected_at")
    @Builder.Default
    private LocalDateTime detectedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SignalStatus status = SignalStatus.NEW;
}
