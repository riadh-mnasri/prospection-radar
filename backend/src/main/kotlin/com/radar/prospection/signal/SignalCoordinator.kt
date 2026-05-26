package com.radar.prospection.signal

import com.radar.prospection.claude.MissionAnalysisService
import com.radar.prospection.repository.SignalRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SignalCoordinator(
    private val detectors: List<SignalDetector>,
    private val signalRepository: SignalRepository,
    private val analysisService: MissionAnalysisService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun runAll(): SignalScanResult {
        var newCount = 0
        val errors = mutableListOf<String>()

        log.info("=== SIGNAL SCAN démarré — {} détecteurs ===", detectors.size)

        for (detector in detectors) {
            try {
                val signals = detector.detect()
                log.info("[{}] {} signaux détectés", detector::class.simpleName, signals.size)

                for (signal in signals) {
                    try {
                        if (signalRepository.existsByCompanyAndTypeAndDetectedAtAfter(
                                signal.company ?: continue,
                                signal.type ?: continue,
                                LocalDateTime.now().minusDays(7)
                            )) continue

                        val analyzed = analysisService.analyzeSignal(signal)
                        signalRepository.save(analyzed)
                        newCount++
                    } catch (e: Exception) {
                        log.warn("Erreur sauvegarde signal: {}", e.message)
                    }
                }
            } catch (e: Exception) {
                log.error("[{}] Détecteur en erreur: {}", detector::class.simpleName, e.message)
                errors.add(e.message ?: "Unknown error")
            }
        }

        log.info("=== SIGNAL SCAN terminé — {} nouveaux signaux ===", newCount)
        return SignalScanResult(newCount, errors)
    }

    data class SignalScanResult(val newSignals: Int, val errors: List<String>)
}
