package com.radar.prospection.claude

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.radar.prospection.api.OutreachDto
import com.radar.prospection.config.RadarProperties
import com.radar.prospection.domain.Mission
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OutreachService(
    private val claude: ClaudeService,
    private val properties: RadarProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SYSTEM_PROMPT = """
            Tu es un expert en prospection commerciale pour freelances Tech Lead Java.
            Tu rédiges des messages de prise de contact percutants, personnalisés et humains.
            Tu retournes UNIQUEMENT un JSON valide, sans aucun texte autour.
            Ton style : direct, professionnel, jamais commercial, toujours axé valeur.
            Le freelance s'appelle Riadh MNASRI.
            """
    }

    fun generate(mission: Mission): OutreachDto {
        val profile = properties.profile
        val descSnippet = mission.description?.take(400) ?: "Non précisée"

        val prompt = """
            PROFIL FREELANCE :
            Nom : Riadh MNASRI
            Rôle : Tech Lead Java / Architecte
            Compétences : ${profile.skills.joinToString(", ")}
            TJM : ${profile.tjmMin}–${profile.tjmMax} €/jour
            Préférence : ${profile.remotePreference}

            MISSION CIBLÉE :
            Titre : ${mission.title}
            Entreprise : ${mission.company ?: "Non précisée"}
            Description : $descSnippet
            Skills requis : ${mission.skills.joinToString(", ").ifEmpty { "Non précisés" }}
            TJM proposé : ${if (mission.tjmMin != null) "${mission.tjmMin}–${mission.tjmMax}€/j" else "Non précisé"}
            Durée : ${mission.duration ?: "Non précisée"}
            Source : ${mission.source}
            Analyse fit : ${mission.fitSummary ?: "Non analysée"}
            Contact suggéré : ${mission.decisionMakerHint ?: "Non identifié"}

            Rédige des messages de prise de contact. LinkedIn = court et percutant (max 300 caractères).
            Email = plus structuré avec un objet accrocheur.

            Retourne UNIQUEMENT ce JSON :
            {
              "linkedinMessage": "<message LinkedIn en français, 2-3 phrases max, direct, valeur immédiate, accroche personnalisée sur la mission>",
              "emailSubject": "<objet email court et percutant>",
              "emailBody": "<corps email en français, 5-8 lignes, intro personnalisée sur la mission, 2-3 bullets sur la valeur apportée, call to action clair>"
            }
            """

        val result = claude.completeAsJson(SYSTEM_PROMPT, prompt, OutreachResult::class.java)
        log.info("[Outreach] Généré pour '{}'", mission.title)
        return OutreachDto(result.linkedinMessage ?: "", result.emailSubject ?: "", result.emailBody ?: "")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OutreachResult(
        val linkedinMessage: String? = null,
        val emailSubject: String? = null,
        val emailBody: String? = null
    )
}
