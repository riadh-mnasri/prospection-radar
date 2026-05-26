package com.radar.prospection.api

import com.radar.prospection.claude.MissionAnalysisService
import com.radar.prospection.claude.OutreachService
import com.radar.prospection.domain.MissionStatus
import com.radar.prospection.domain.SignalStatus
import com.radar.prospection.repository.MissionRepository
import com.radar.prospection.repository.SignalRepository
import com.radar.prospection.scraper.ScraperCoordinator
import com.radar.prospection.signal.SignalCoordinator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/radar")
class RadarController(
    private val missionRepository: MissionRepository,
    private val signalRepository: SignalRepository,
    private val scraperCoordinator: ScraperCoordinator,
    private val signalCoordinator: SignalCoordinator,
    private val analysisService: MissionAnalysisService,
    private val outreachService: OutreachService
) {

    @GetMapping("/missions")
    fun getMissions(
        @RequestParam(defaultValue = "0") minScore: Int,
        @RequestParam(required = false) status: String?
    ): List<MissionDto> {
        val missions = when {
            minScore > 0 -> missionRepository.findByFitScoreGreaterThanEqualOrderByFitScoreDesc(minScore)
            status != null -> missionRepository.findByStatusOrderByFitScoreDesc(MissionStatus.valueOf(status))
            else -> missionRepository.findRecentMissions(LocalDateTime.now().minusDays(7))
        }
        return missions.map { MissionDto.from(it) }
    }

    @GetMapping("/missions/top")
    fun getTopMissions(): List<MissionDto> =
        missionRepository.findByFitScoreGreaterThanEqualOrderByFitScoreDesc(70).map { MissionDto.from(it) }

    @PatchMapping("/missions/{id}/status")
    fun updateStatus(@PathVariable id: Long, @RequestBody body: Map<String, String>): ResponseEntity<MissionDto> =
        missionRepository.findById(id).map { mission ->
            mission.status = MissionStatus.valueOf(body["status"]!!)
            ResponseEntity.ok(MissionDto.from(missionRepository.save(mission)))
        }.orElse(ResponseEntity.notFound().build())

    @GetMapping("/signals")
    fun getSignals(@RequestParam(defaultValue = "0") minScore: Int): List<SignalDto> {
        val signals = if (minScore > 0)
            signalRepository.findHighValueSignals(minScore)
        else
            signalRepository.findByStatusOrderByOpportunityScoreDesc(SignalStatus.NEW)
        return signals.map { SignalDto.from(it) }
    }

    @GetMapping("/signals/hot")
    fun getHotSignals(): List<SignalDto> =
        signalRepository.findHighValueSignals(60).map { SignalDto.from(it) }

    @PostMapping("/scan/missions")
    fun triggerMissionScan(): ResponseEntity<Map<String, Any>> {
        val result = scraperCoordinator.runAll()
        return ResponseEntity.ok(mapOf(
            "newMissions" to result.newMissions,
            "duplicates" to result.duplicates,
            "analyzed" to result.analyzed,
            "errors" to result.errors
        ))
    }

    @PostMapping("/reanalyze")
    fun reanalyzeMissions(): ResponseEntity<Map<String, Any>> {
        val pending = missionRepository.findByFitScoreIsNull()
        var count = 0
        for (m in pending) {
            try {
                missionRepository.save(analysisService.analyze(m))
                count++
            } catch (_: Exception) {}
        }
        return ResponseEntity.ok(mapOf("reanalyzed" to count, "total" to pending.size))
    }

    @GetMapping("/missions/favorites")
    fun getFavorites(): List<MissionDto> =
        missionRepository.findByFavoriteTrueOrderByFitScoreDesc().map { MissionDto.from(it) }

    @PatchMapping("/missions/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Long, @RequestBody body: Map<String, Boolean>): ResponseEntity<MissionDto> =
        missionRepository.findById(id).map { mission ->
            mission.favorite = body["favorite"] ?: false
            ResponseEntity.ok(MissionDto.from(missionRepository.save(mission)))
        }.orElse(ResponseEntity.notFound().build())

    @PostMapping("/missions/{id}/outreach")
    fun generateOutreach(@PathVariable id: Long): ResponseEntity<OutreachDto> =
        missionRepository.findById(id)
            .map { ResponseEntity.ok(outreachService.generate(it)) }
            .orElse(ResponseEntity.notFound().build())

    @PostMapping("/scan/signals")
    fun triggerSignalScan(): ResponseEntity<Map<String, Any>> {
        val result = signalCoordinator.runAll()
        return ResponseEntity.ok(mapOf(
            "newSignals" to result.newSignals,
            "errors" to result.errors
        ))
    }

    @GetMapping("/stats")
    fun getStats(): Map<String, Any> {
        val since = LocalDateTime.now().minusDays(7)
        val recent = missionRepository.findRecentMissions(since)
        val hotSignals = signalRepository.findHighValueSignals(60)

        val topMissions = recent.count { it.fitScore != null && it.fitScore!! >= 70 }
        val avgScore = recent.filter { it.fitScore != null }.map { it.fitScore!! }.average().let {
            if (it.isNaN()) 0.0 else it
        }

        return mapOf(
            "missionsLast7Days" to recent.size,
            "topMissions" to topMissions,
            "avgFitScore" to Math.round(avgScore),
            "hotSignals" to hotSignals.size,
            "missionsBySource" to recent.groupingBy { it.source?.name ?: "UNKNOWN" }.eachCount()
        )
    }
}
