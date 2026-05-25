package com.radar.prospection.scheduler;

import com.radar.prospection.scraper.ScraperCoordinator;
import com.radar.prospection.signal.SignalCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RadarScheduler {

    private final ScraperCoordinator scraperCoordinator;
    private final SignalCoordinator signalCoordinator;

    // 3x par jour : 8h, 12h, 18h
    @Scheduled(cron = "${radar.scheduling.job-boards-cron}")
    public void scanJobBoards() {
        log.info("⏰ Scan job boards déclenché");
        ScraperCoordinator.ScrapingResult result = scraperCoordinator.runAll();
        log.info("✅ Job boards : {} nouvelles missions (score fit calculé par Claude)", result.newMissions());
    }

    // 1x par jour à 8h30
    @Scheduled(cron = "${radar.scheduling.signals-cron}")
    public void scanSignals() {
        log.info("⏰ Scan signaux marché caché déclenché");
        SignalCoordinator.SignalScanResult result = signalCoordinator.runAll();
        log.info("✅ Signaux : {} nouveaux signaux détectés", result.newSignals());
    }
}
