package com.radar.prospection.claude;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.radar.prospection.api.OutreachDto;
import com.radar.prospection.config.RadarProperties;
import com.radar.prospection.domain.Mission;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutreachService {

    private final ClaudeService claude;
    private final RadarProperties properties;

    private static final String SYSTEM_PROMPT = """
        Tu es un expert en prospection commerciale pour freelances Tech Lead Java.
        Tu rédiges des messages de prise de contact percutants, personnalisés et humains.
        Tu retournes UNIQUEMENT un JSON valide, sans aucun texte autour.
        Ton style : direct, professionnel, jamais commercial, toujours axé valeur.
        Le freelance s'appelle Riadh MNASRI.
        """;

    public OutreachDto generate(Mission mission) {
        RadarProperties.Profile profile = properties.getProfile();

        String prompt = """
            PROFIL FREELANCE :
            Nom : Riadh MNASRI
            Rôle : Tech Lead Java / Architecte
            Compétences : %s
            TJM : %d–%d €/jour
            Préférence : %s

            MISSION CIBLÉE :
            Titre : %s
            Entreprise : %s
            Description : %s
            Skills requis : %s
            TJM proposé : %s
            Durée : %s
            Source : %s
            Analyse fit : %s
            Contact suggéré : %s

            Rédige des messages de prise de contact. LinkedIn = court et percutant (max 300 caractères).
            Email = plus structuré avec un objet accrocheur.

            Retourne UNIQUEMENT ce JSON :
            {
              "linkedinMessage": "<message LinkedIn en français, 2-3 phrases max, direct, valeur immédiate, accroche personnalisée sur la mission>",
              "emailSubject": "<objet email court et percutant>",
              "emailBody": "<corps email en français, 5-8 lignes, intro personnalisée sur la mission, 2-3 bullets sur la valeur apportée, call to action clair>"
            }
            """.formatted(
                String.join(", ", profile.getSkills()),
                profile.getTjmMin(), profile.getTjmMax(),
                profile.getRemotePreference(),
                mission.getTitle(),
                mission.getCompany() != null ? mission.getCompany() : "Non précisée",
                mission.getDescription() != null ? mission.getDescription().substring(0, Math.min(400, mission.getDescription().length())) : "Non précisée",
                mission.getSkills() != null ? String.join(", ", mission.getSkills()) : "Non précisés",
                mission.getTjmMin() != null ? mission.getTjmMin() + "–" + mission.getTjmMax() + "€/j" : "Non précisé",
                mission.getDuration() != null ? mission.getDuration() : "Non précisée",
                mission.getSource(),
                mission.getFitSummary() != null ? mission.getFitSummary() : "Non analysée",
                mission.getDecisionMakerHint() != null ? mission.getDecisionMakerHint() : "Non identifié"
            );

        OutreachResult result = claude.completeAsJson(SYSTEM_PROMPT, prompt, OutreachResult.class);
        log.info("[Outreach] Généré pour '{}'", mission.getTitle());
        return new OutreachDto(result.getLinkedinMessage(), result.getEmailSubject(), result.getEmailBody());
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutreachResult {
        private String linkedinMessage;
        private String emailSubject;
        private String emailBody;
    }
}
