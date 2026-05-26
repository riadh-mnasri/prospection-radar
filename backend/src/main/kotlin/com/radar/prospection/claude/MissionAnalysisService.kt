package com.radar.prospection.claude

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.radar.prospection.config.RadarProperties
import com.radar.prospection.domain.Mission
import com.radar.prospection.domain.MissionStatus
import com.radar.prospection.domain.Signal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MissionAnalysisService(
    private val claude: ClaudeService,
    private val properties: RadarProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MISSION_SYSTEM_PROMPT = """
            Tu es un assistant spécialisé pour les freelances Java Tech Lead.
            Tu analyses des offres de missions freelance et tu retournes UNIQUEMENT un JSON valide, sans aucun texte autour.

            Ton rôle :
            1. Évaluer le fit de la mission avec le profil du freelance (score 0-100)
            2. Identifier qui contacter en priorité (décideur vs RH vs manager)
            3. Détecter les éléments différenciants pour personnaliser l'approche
            4. Identifier les signaux d'urgence (mission ouverte depuis longtemps, langage urgent)
            """

        private const val SIGNAL_SYSTEM_PROMPT = """
            Tu es un expert en prospection B2B pour freelances tech.
            Tu analyses des signaux du marché caché et tu retournes UNIQUEMENT un JSON valide, sans texte autour.

            Pour chaque signal, évalue :
            1. Le score d'opportunité (0-100) pour un Tech Lead Java freelance
            2. Pourquoi c'est une opportunité
            3. L'action recommandée et le timing
            """
    }

    fun analyze(mission: Mission): Mission {
        try {
            val profile = properties.profile
            val prompt = buildProfileContext(profile) + "\n\n" + buildMissionContext(mission) + "\n\n" + """
                Retourne UNIQUEMENT ce JSON :
                {
                  "fitScore": <0-100>,
                  "fitSummary": "<3-4 phrases : pourquoi c'est un bon/mauvais fit, points forts, points faibles>",
                  "decisionMakerHint": "<qui contacter : rôle, comment l'approcher, angle d'accroche>",
                  "urgencySignals": ["<signal1>", "<signal2>"],
                  "recommendedAction": "<APPLY|RESEARCH_MORE|SKIP>"
                }
                """

            val result = claude.completeAsJson(MISSION_SYSTEM_PROMPT, prompt, MissionAnalysisResult::class.java)

            mission.fitScore = result.fitScore
            mission.fitSummary = result.fitSummary
            mission.decisionMakerHint = result.decisionMakerHint
            mission.status = MissionStatus.ANALYZED
            mission.analyzedAt = LocalDateTime.now()

            log.info("[Analyse] '{}' → score: {}", mission.title, result.fitScore)

        } catch (e: Exception) {
            log.warn("[Analyse] Echec pour '{}': {}", mission.title, e.message)
            mission.fitSummary = "Analyse impossible: ${e.message}"
            mission.status = MissionStatus.ANALYZED
        }

        return mission
    }

    fun analyzeSignal(signal: Signal): Signal {
        try {
            val prompt = """
                Signal détecté :
                Type : ${signal.type}
                Entreprise : ${signal.company}
                Titre : ${signal.title}
                Description : ${signal.description}

                Retourne UNIQUEMENT ce JSON :
                {
                  "opportunityScore": <0-100>,
                  "opportunityReason": "<pourquoi c'est une opportunité pour un Tech Lead Java freelance>",
                  "suggestedAction": "<action concrète à faire dans les 48h>",
                  "bestContactRole": "<CTO|VP_ENG|TEAM_LEAD|HR|FOUNDER>",
                  "timingAdvice": "<quand contacter et pourquoi>"
                }
                """

            val result = claude.completeAsJson(SIGNAL_SYSTEM_PROMPT, prompt, SignalAnalysisResult::class.java)

            signal.opportunityScore = result.opportunityScore
            signal.opportunityReason = result.opportunityReason
            signal.suggestedAction = result.suggestedAction

            log.info("[Signal] '{}' → score: {}", signal.company, result.opportunityScore)

        } catch (e: Exception) {
            log.warn("[Signal] Echec pour '{}': {}", signal.company, e.message)
        }

        return signal
    }

    private fun buildProfileContext(profile: RadarProperties.Profile) = """
        PROFIL FREELANCE :
        Compétences : ${profile.skills.joinToString(", ")}
        TJM cible : ${profile.tjmMin} - ${profile.tjmMax} €/jour
        Préférence télétravail : ${profile.remotePreference}
        Localisation : ${profile.location}
        """

    private fun buildMissionContext(mission: Mission) = """
        MISSION À ANALYSER :
        Titre : ${mission.title}
        Description : ${mission.description ?: "Non précisée"}
        Skills requis : ${mission.skills.joinToString(", ").ifEmpty { "Non précisés" }}
        TJM proposé : ${mission.tjmMin ?: 0} - ${mission.tjmMax ?: 0} €/jour
        Durée : ${mission.duration ?: "Non précisée"}
        Localisation : ${mission.location ?: "Non précisée"}
        Télétravail : ${if (mission.remote == true) "Oui" else "Non/Hybride"}
        Source : ${mission.source}
        """

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MissionAnalysisResult(
        val fitScore: Int = 0,
        val fitSummary: String? = null,
        val decisionMakerHint: String? = null,
        val urgencySignals: List<String> = emptyList(),
        val recommendedAction: String? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class SignalAnalysisResult(
        val opportunityScore: Int = 0,
        val opportunityReason: String? = null,
        val suggestedAction: String? = null,
        val bestContactRole: String? = null,
        val timingAdvice: String? = null
    )
}
