package com.radar.prospection.api

import com.radar.prospection.domain.Mission
import com.radar.prospection.domain.MissionStatus
import com.radar.prospection.domain.Source
import java.time.LocalDateTime

data class MissionDto(
    val id: Long?,
    val source: Source?,
    val title: String?,
    val company: String?,
    val location: String?,
    val remote: Boolean?,
    val tjmMin: Int?,
    val tjmMax: Int?,
    val duration: String?,
    val skills: List<String>,
    val url: String?,
    val status: MissionStatus?,
    val fitScore: Int?,
    val fitSummary: String?,
    val decisionMakerHint: String?,
    val detectedAt: LocalDateTime?,
    val favorite: Boolean
) {
    companion object {
        fun from(m: Mission) = MissionDto(
            id = m.id,
            source = m.source,
            title = m.title,
            company = m.company,
            location = m.location,
            remote = m.remote,
            tjmMin = m.tjmMin,
            tjmMax = m.tjmMax,
            duration = m.duration,
            skills = m.skills,
            url = m.url,
            status = m.status,
            fitScore = m.fitScore,
            fitSummary = m.fitSummary,
            decisionMakerHint = m.decisionMakerHint,
            detectedAt = m.detectedAt,
            favorite = m.favorite
        )
    }
}
