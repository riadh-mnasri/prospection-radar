package com.radar.prospection.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "missions", uniqueConstraints = [UniqueConstraint(columnNames = ["source", "external_id"])])
class Mission(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Enumerated(EnumType.STRING)
    var source: Source? = null,

    @Column(name = "external_id")
    var externalId: String? = null,

    var title: String? = null,

    @Column(length = 5000)
    var description: String? = null,

    var company: String? = null,
    var location: String? = null,
    var remote: Boolean? = null,
    var tjmMin: Int? = null,
    var tjmMax: Int? = null,
    var duration: String? = null,
    var startDate: String? = null,

    @ElementCollection
    @CollectionTable(name = "mission_skills", joinColumns = [JoinColumn(name = "mission_id")])
    @Column(name = "skill")
    var skills: MutableList<String> = mutableListOf(),

    var url: String? = null,

    @Enumerated(EnumType.STRING)
    var status: MissionStatus = MissionStatus.NEW,

    @Column(name = "fit_score")
    var fitScore: Int? = null,

    @Column(name = "fit_summary", length = 2000)
    var fitSummary: String? = null,

    @Column(name = "decision_maker_hint", length = 500)
    var decisionMakerHint: String? = null,

    @Column(name = "detected_at")
    var detectedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "analyzed_at")
    var analyzedAt: LocalDateTime? = null,

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null,

    @Column(name = "favorite")
    var favorite: Boolean = false
)
