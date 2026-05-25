package com.radar.prospection.signal;

import com.radar.prospection.claude.MissionAnalysisService;
import com.radar.prospection.domain.Signal;
import com.radar.prospection.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignalCoordinator {

    private final List<SignalDetector> detectors;
    private final SignalRepository signalRepository;
    private final MissionAnalysisService analysisService;

    public SignalScanResult runAll() {
        int newCount = 0;
        List<String> errors = new ArrayList<>();

        log.info("=== SIGNAL SCAN démarré — {} détecteurs ===", detectors.size());

        for (SignalDetector detector : detectors) {
            try {
                List<Signal> signals = detector.detect();
                log.info("[{}] {} signaux détectés", detector.getClass().getSimpleName(), signals.size());

                for (Signal signal : signals) {
                    try {
                        // Eviter les doublons (même entreprise + même type dans les 7 derniers jours)
                        if (signalRepository.existsByCompanyAndTypeAndDetectedAtAfter(
                                signal.getCompany(), signal.getType(),
                                LocalDateTime.now().minusDays(7))) {
                            continue;
                        }

                        Signal analyzed = analysisService.analyzeSignal(signal);
                        signalRepository.save(analyzed);
                        newCount++;

                    } catch (Exception e) {
                        log.warn("Erreur sauvegarde signal: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("[{}] Détecteur en erreur: {}", detector.getClass().getSimpleName(), e.getMessage());
                errors.add(e.getMessage());
            }
        }

        log.info("=== SIGNAL SCAN terminé — {} nouveaux signaux ===", newCount);
        return new SignalScanResult(newCount, errors);
    }

    public record SignalScanResult(int newSignals, List<String> errors) {}
}
