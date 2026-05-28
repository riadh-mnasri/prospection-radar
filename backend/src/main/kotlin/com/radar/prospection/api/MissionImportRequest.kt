package com.radar.prospection.api

data class MissionImportRequest(
    val title: String,
    val description: String? = null,
    val company: String? = null,
    val location: String? = null,
    val remote: Boolean? = null,
    val tjmMin: Int? = null,
    val tjmMax: Int? = null,
    val duration: String? = null,
    val skills: List<String> = emptyList(),
    val url: String? = null
)
