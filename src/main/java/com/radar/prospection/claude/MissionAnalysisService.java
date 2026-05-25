package com.radar.prospection.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.radar.prospection.config.RadarProperties;
import com.radar.prospection.domain.Mission;
import com.radar.prospection.domain.MissionStatus;
import com.radar.prospection.domain.Signal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionAnalysisService {

    private final ClaudeService claude;
    private final RadarProperties properties;

    private static final String MISSION_SYSTEM_PROMPT = """
        Tu es un assistant spécialisé pour les freelances Java Tech Lead.
        Tu analyses des offres de missions freelance et tu retournes UNIQUEMENT un JSON valide, sans aucun texte autour.

        Ton rôle :
        1. Évaluer le fit de la mission avec le profil du freelance (score 0-100)
        2. Identifier qui contacter en priorité (décideur vs RH vs manager)
        3. Détecter les éléments différenciants pour personnaliser l'approche
        4. Identifier les signaux d'urgence (mission ouverte depuis longtemps, langage urgent)
        """;

    private static final String SIGNAL_SYSTEM_PROMPT = """
        Tu es un expert en prospection B2B pour freelances tech.
        Tu analyses des signaux du marché caché et tu retournes UNIQUEMENT un JSON valide, sans texte autour.

        Pour chaque signal, évalue :
        1. Le score d'opportunité (0-100) pour un Tech Lead Java freelance
        2. Pourquoi c'est une opportunité
        3. L'action recommandée et le timing
        """;

    public Mission analyze(Mission mission) {
        try {
            RadarProperties.Profile profile = properties.getProfile();
            String profileContext = buildProfileContext(profile);
            String missionContext = buildMissionContext(mission);

            String prompt = profileContext + "\n\n" + missionContext + "\n\n" +
                """
                Retourne UNIQUEMENT ce JSON :
                {
                  "fitScore": <0-100>,
                  "fitSummary": "<3-4 phrases : pourquoi c'est un bon/mauvais fit, points forts, points faibles>",
                  "decisionMakerHint": "<qui contacter : rôle, comment l'approcher, angle d'accroche>",
                  "urgencySignals": ["<signal1>", "<signal2>"],
                  "recommendedAction": "<APPLY|RESEARCH_MORE|SKIP>"
                }
                """;

            MissionAnalysisResult result = claude.completeAsJson(
                MISSION_SYSTEM_PROMPT, prompt, MissionAnalysisResult.class
            );

            mission.setFitScore(result.getFitScore());
            mission.setFitSummary(result.getFitSummary());
            mission.setDecisionMakerHint(result.getDecisionMakerHint());
            mission.setStatus(MissionStatus.ANALYZED);
            mission.setAnalyzedAt(LocalDateTime.now());

            log.info("[Analyse] '{}' → score: {}", mission.getTitle(), result.getFitScore());

        } catch (Exception e) {
            log.warn("[Analyse] Echec pour '{}': {}", mission.getTitle(), e.getMessage());
            mission.setFitSummary("Analyse impossible: " + e.getMessage());
            mission.setStatus(MissionStatus.ANALYZED);
        }

        return mission;
    }

    public Signal analyzeSignal(Signal signal) {
        try {
            String prompt = """
                Signal détecté :
                Type : %s
                Entreprise : %s
                Titre : %s
                Description : %s

                Retourne UNIQUEMENT ce JSON :
                {
                  "opportunityScore": <0-100>,
                  "opportunityReason": "<pourquoi c'est une opportunité pour un Tech Lead Java freelance>",
                  "suggestedAction": "<action concrète à faire dans les 48h>",
                  "bestContactRole": "<CTO|VP_ENG|TEAM_LEAD|HR|FOUNDER>",
                  "timingAdvice": "<quand contacter et pourquoi>"
                }
                """.formatted(
                    signal.getType(),
                    signal.getCompany(),
                    signal.getTitle(),
                    signal.getDescription()
                );

            SignalAnalysisResult result = claude.completeAsJson(
                SIGNAL_SYSTEM_PROMPT, prompt, SignalAnalysisResult.class
            );

            signal.setOpportunityScore(result.getOpportunityScore());
            signal.setOpportunityReason(result.getOpportunityReason());
            signal.setSuggestedAction(result.getSuggestedAction());

            log.info("[Signal] '{}' → score: {}", signal.getCompany(), result.getOpportunityScore());

        } catch (Exception e) {
            log.warn("[Signal] Echec pour '{}': {}", signal.getCompany(), e.getMessage());
        }

        return signal;
    }

    private String buildProfileContext(RadarProperties.Profile profile) {
        return """
            PROFIL FREELANCE :
            Compétences : %s
            TJM cible : %d - %d €/jour
            Préférence télétravail : %s
            Localisation : %s
            """.formatted(
                String.join(", ", profile.getSkills()),
                profile.getTjmMin(),
                profile.getTjmMax(),
                profile.getRemotePreference(),
                profile.getLocation()
            );
    }

    private String buildMissionContext(Mission mission) {
        return """
            MISSION À ANALYSER :
            Titre : %s
            Description : %s
            Skills requis : %s
            TJM proposé : %d - %d €/jour
            Durée : %s
            Localisation : %s
            Télétravail : %s
            Source : %s
            """.formatted(
                mission.getTitle(),
                mission.getDescription() != null ? mission.getDescription() : "Non précisée",
                mission.getSkills() != null ? String.join(", ", mission.getSkills()) : "Non précisés",
                mission.getTjmMin() != null ? mission.getTjmMin() : 0,
                mission.getTjmMax() != null ? mission.getTjmMax() : 0,
                mission.getDuration() != null ? mission.getDuration() : "Non précisée",
                mission.getLocation() != null ? mission.getLocation() : "Non précisée",
                Boolean.TRUE.equals(mission.getRemote()) ? "Oui" : "Non/Hybride",
                mission.getSource()
            );
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MissionAnalysisResult {
        private int fitScore;
        private String fitSummary;
        private String decisionMakerHint;
        private List<String> urgencySignals;
        private String recommendedAction;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SignalAnalysisResult {
        private int opportunityScore;
        private String opportunityReason;
        private String suggestedAction;
        private String bestContactRole;
        private String timingAdvice;
    }
}
