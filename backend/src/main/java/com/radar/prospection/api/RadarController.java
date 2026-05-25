package com.radar.prospection.api;

import com.radar.prospection.domain.*;
import com.radar.prospection.repository.MissionRepository;
import com.radar.prospection.repository.SignalRepository;
import com.radar.prospection.scraper.ScraperCoordinator;
import com.radar.prospection.signal.SignalCoordinator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/radar")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RadarController {

    private final MissionRepository missionRepository;
    private final SignalRepository signalRepository;
    private final ScraperCoordinator scraperCoordinator;
    private final SignalCoordinator signalCoordinator;

    // ──────────────────────────────────────────
    // MISSIONS
    // ──────────────────────────────────────────

    @GetMapping("/missions")
    public List<MissionDto> getMissions(
            @RequestParam(defaultValue = "0") int minScore,
            @RequestParam(required = false) String status) {

        List<Mission> missions = minScore > 0
                ? missionRepository.findByFitScoreGreaterThanEqualOrderByFitScoreDesc(minScore)
                : status != null
                    ? missionRepository.findByStatusOrderByFitScoreDesc(MissionStatus.valueOf(status))
                    : missionRepository.findRecentMissions(LocalDateTime.now().minusDays(7));

        return missions.stream().map(MissionDto::from).toList();
    }

    @GetMapping("/missions/top")
    public List<MissionDto> getTopMissions() {
        return missionRepository.findByFitScoreGreaterThanEqualOrderByFitScoreDesc(70)
                .stream().map(MissionDto::from).toList();
    }

    @PatchMapping("/missions/{id}/status")
    public ResponseEntity<MissionDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        return missionRepository.findById(id).map(mission -> {
            mission.setStatus(MissionStatus.valueOf(body.get("status")));
            return ResponseEntity.ok(MissionDto.from(missionRepository.save(mission)));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ──────────────────────────────────────────
    // SIGNAUX
    // ──────────────────────────────────────────

    @GetMapping("/signals")
    public List<SignalDto> getSignals(
            @RequestParam(defaultValue = "0") int minScore) {

        return (minScore > 0
                ? signalRepository.findHighValueSignals(minScore)
                : signalRepository.findByStatusOrderByOpportunityScoreDesc(SignalStatus.NEW))
                .stream().map(SignalDto::from).toList();
    }

    @GetMapping("/signals/hot")
    public List<SignalDto> getHotSignals() {
        return signalRepository.findHighValueSignals(60)
                .stream().map(SignalDto::from).toList();
    }

    // ──────────────────────────────────────────
    // ACTIONS MANUELLES
    // ──────────────────────────────────────────

    @PostMapping("/scan/missions")
    public ResponseEntity<Map<String, Object>> triggerMissionScan() {
        ScraperCoordinator.ScrapingResult result = scraperCoordinator.runAll();
        return ResponseEntity.ok(Map.of(
            "newMissions", result.newMissions(),
            "duplicates", result.duplicates(),
            "analyzed", result.analyzed(),
            "errors", result.errors()
        ));
    }

    @PostMapping("/scan/signals")
    public ResponseEntity<Map<String, Object>> triggerSignalScan() {
        SignalCoordinator.SignalScanResult result = signalCoordinator.runAll();
        return ResponseEntity.ok(Map.of(
            "newSignals", result.newSignals(),
            "errors", result.errors()
        ));
    }

    // ──────────────────────────────────────────
    // DASHBOARD STATS
    // ──────────────────────────────────────────

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Mission> recent = missionRepository.findRecentMissions(since);
        List<Signal> hotSignals = signalRepository.findHighValueSignals(60);

        long topMissions = recent.stream().filter(m -> m.getFitScore() != null && m.getFitScore() >= 70).count();
        double avgScore = recent.stream()
                .filter(m -> m.getFitScore() != null)
                .mapToInt(Mission::getFitScore)
                .average().orElse(0);

        return Map.of(
            "missionsLast7Days", recent.size(),
            "topMissions", topMissions,
            "avgFitScore", Math.round(avgScore),
            "hotSignals", hotSignals.size(),
            "missionsBySource", countBySource(recent)
        );
    }

    private Map<String, Long> countBySource(List<Mission> missions) {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Mission m : missions) {
            counts.merge(m.getSource().name(), 1L, Long::sum);
        }
        return counts;
    }
}
