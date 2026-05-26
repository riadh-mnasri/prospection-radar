package com.radar.prospection.api

import com.radar.prospection.domain.Signal
import com.radar.prospection.domain.SignalType
import java.time.LocalDateTime

data class SignalDto(
    val id: Long?,
    val type: SignalType?,
    val company: String?,
    val title: String?,
    val description: String?,
    val fundingAmount: String?,
    val personName: String?,
    val newRole: String?,
    val opportunityScore: Int?,
    val opportunityReason: String?,
    val suggestedAction: String?,
    val url: String?,
    val detectedAt: LocalDateTime
) {
    companion object {
        fun from(s: Signal) = SignalDto(
            id = s.id,
            type = s.type,
            company = s.company,
            title = s.title,
            description = s.description,
            fundingAmount = s.fundingAmount,
            personName = s.personName,
            newRole = s.newRole,
            opportunityScore = s.opportunityScore,
            opportunityReason = s.opportunityReason,
            suggestedAction = s.suggestedAction,
            url = s.url,
            detectedAt = s.detectedAt
        )
    }
}
