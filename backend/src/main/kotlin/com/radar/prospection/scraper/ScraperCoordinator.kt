package com.radar.prospection.scraper

import com.radar.prospection.claude.MissionAnalysisService
import com.radar.prospection.config.RadarProperties
import com.radar.prospection.notification.NotificationService
import com.radar.prospection.repository.MissionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ScraperCoordinator(
    private val scrapers: List<MissionScraper>,
    private val missionRepository: MissionRepository,
    private val analysisService: MissionAnalysisService,
    private val properties: RadarProperties,
    private val notificationService: NotificationService? = null
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun runAll(): ScrapingResult {
        val keywords = properties.scraping.keywords
        var newCount = 0
        var duplicateCount = 0
        var analyzedCount = 0
        val errors = mutableListOf<String>()

        log.info("=== RADAR SCAN démarré — {} scrapers, {} keywords ===", scrapers.size, keywords.size)

        for (scraper in scrapers) {
            log.info("--- Scraper: {} ---", scraper.getSource())
            try {
                val found = scraper.scrape(keywords)
                log.info("[{}] {} missions brutes trouvées", scraper.getSource(), found.size)

                for (mission in found) {
                    try {
                        if (missionRepository.existsBySourceAndExternalId(mission.source!!, mission.externalId!!)) {
                            duplicateCount++
                            continue
                        }
                        val analyzed = analysisService.analyze(mission)
                        val saved = missionRepository.save(analyzed)
                        notificationService?.notifyHighScoreMission(saved)
                        newCount++
                        analyzedCount++
                    } catch (e: Exception) {
                        log.warn("[{}] Erreur sauvegarde mission '{}': {}", scraper.getSource(), mission.title, e.message)
                        errors.add("${scraper.getSource()}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                log.error("[{}] Scraper en erreur: {}", scraper.getSource(), e.message)
                errors.add("${scraper.getSource()} FATAL: ${e.message}")
            }
        }

        log.info("=== RADAR SCAN terminé — {} nouvelles, {} doublons, {} erreurs ===", newCount, duplicateCount, errors.size)
        return ScrapingResult(newCount, duplicateCount, analyzedCount, errors)
    }

    data class ScrapingResult(
        val newMissions: Int,
        val duplicates: Int,
        val analyzed: Int,
        val errors: List<String>
    )
}
