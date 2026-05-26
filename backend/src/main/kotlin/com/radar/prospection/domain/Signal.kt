package com.radar.prospection.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "signals")
class Signal(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    var type: SignalType? = null,

    var company: String? = null,
    var title: String? = null,

    @Column(length = 3000)
    var description: String? = null,

    var fundingAmount: String? = null,
    var fundingRound: String? = null,
    var personName: String? = null,
    var newRole: String? = null,

    @Column(name = "opportunity_score")
    var opportunityScore: Int? = null,

    @Column(name = "opportunity_reason", length = 1000)
    var opportunityReason: String? = null,

    @Column(name = "suggested_action", length = 500)
    var suggestedAction: String? = null,

    var url: String? = null,
    var sourceWebsite: String? = null,

    @Column(name = "detected_at")
    var detectedAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    var status: SignalStatus = SignalStatus.NEW
)
