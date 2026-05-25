package com.radar.prospection.scraper;

import com.radar.prospection.claude.MissionAnalysisService;
import com.radar.prospection.config.RadarProperties;
import com.radar.prospection.domain.Mission;
import com.radar.prospection.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperCoordinator {

    private final List<MissionScraper> scrapers;
    private final MissionRepository missionRepository;
    private final MissionAnalysisService analysisService;
    private final RadarProperties properties;

    public ScrapingResult runAll() {
        List<String> keywords = properties.getScraping().getKeywords();
        AtomicInteger newCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger analyzedCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        log.info("=== RADAR SCAN démarré — {} scrapers, {} keywords ===",
            scrapers.size(), keywords.size());

        for (MissionScraper scraper : scrapers) {
            log.info("--- Scraper: {} ---", scraper.getSource());
            try {
                List<Mission> found = scraper.scrape(keywords);
                log.info("[{}] {} missions brutes trouvées", scraper.getSource(), found.size());

                for (Mission mission : found) {
                    try {
                        if (missionRepository.existsBySourceAndExternalId(
                                mission.getSource(), mission.getExternalId())) {
                            duplicateCount.incrementAndGet();
                            continue;
                        }

                        // Analyse Claude avant sauvegarde
                        Mission analyzed = analysisService.analyze(mission);
                        missionRepository.save(analyzed);
                        newCount.incrementAndGet();
                        analyzedCount.incrementAndGet();

                    } catch (Exception e) {
                        log.warn("[{}] Erreur sauvegarde mission '{}': {}",
                            scraper.getSource(), mission.getTitle(), e.getMessage());
                        errors.add(scraper.getSource() + ": " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("[{}] Scraper en erreur: {}", scraper.getSource(), e.getMessage());
                errors.add(scraper.getSource() + " FATAL: " + e.getMessage());
            }
        }

        log.info("=== RADAR SCAN terminé — {} nouvelles, {} doublons, {} erreurs ===",
            newCount.get(), duplicateCount.get(), errors.size());

        return new ScrapingResult(newCount.get(), duplicateCount.get(), analyzedCount.get(), errors);
    }

    public record ScrapingResult(
        int newMissions,
        int duplicates,
        int analyzed,
        List<String> errors
    ) {}
}
