package com.radar.prospection.scheduler

import com.radar.prospection.scraper.ScraperCoordinator
import com.radar.prospection.signal.SignalCoordinator
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RadarScheduler(
    private val scraperCoordinator: ScraperCoordinator,
    private val signalCoordinator: SignalCoordinator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${radar.scheduling.job-boards-cron}")
    fun scanJobBoards() {
        log.info("Scan job boards déclenché")
        val result = scraperCoordinator.runAll()
        log.info("Job boards : {} nouvelles missions", result.newMissions)
    }

    @Scheduled(cron = "\${radar.scheduling.signals-cron}")
    fun scanSignals() {
        log.info("Scan signaux marché caché déclenché")
        val result = signalCoordinator.runAll()
        log.info("Signaux : {} nouveaux signaux détectés", result.newSignals)
    }
}
