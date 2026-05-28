package com.radar.prospection.notification

import com.radar.prospection.domain.Mission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["notification.email.enabled"], havingValue = "true", matchIfMissing = false)
class NotificationService(
    private val mailSender: JavaMailSender,
    @Value("\${notification.email.to}") private val to: String,
    @Value("\${notification.email.from}") private val from: String,
    @Value("\${notification.email.score-threshold:80}") private val threshold: Int,
    @Value("\${notification.email.app-url:https://frontend-kappa-lilac-83.vercel.app}") private val appUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun notifyHighScoreMission(mission: Mission) {
        val score = mission.fitScore ?: return
        if (score < threshold) return

        try {
            val msg = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(msg, true, "UTF-8")
            helper.setTo(to)
            helper.setFrom(from)
            helper.setSubject("Radar — Mission ${score}/100 : ${mission.title}")
            helper.setText(buildHtml(mission, score), true)
            mailSender.send(msg)
            log.info("[Notif] Email envoyé pour '{}' (score {})", mission.title, score)
        } catch (e: Exception) {
            log.warn("[Notif] Échec envoi email: {}", e.message)
        }
    }

    private fun buildHtml(m: Mission, score: Int): String {
        val scoreColor = if (score >= 85) "#34d399" else if (score >= 70) "#fbbf24" else "#f87171"
        val tjm = if (m.tjmMin != null) "${m.tjmMin}–${m.tjmMax ?: m.tjmMin} €/j" else "Non précisé"
        val detailUrl = "$appUrl/missions/${m.id}"

        return """
        <!DOCTYPE html>
        <html>
        <body style="font-family: -apple-system, sans-serif; background: #0f172a; color: #e2e8f0; padding: 32px; margin: 0;">
          <div style="max-width: 560px; margin: 0 auto;">
            <div style="background: #1e293b; border: 1px solid #334155; border-radius: 16px; padding: 28px;">

              <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 24px;">
                <div style="width: 56px; height: 56px; border-radius: 50%; background: ${scoreColor}22;
                            border: 2px solid $scoreColor; display: flex; align-items: center; justify-content: center;
                            font-size: 18px; font-weight: 800; color: $scoreColor; text-align: center; line-height: 56px;">
                  $score
                </div>
                <div>
                  <div style="font-size: 11px; color: #64748b; font-weight: 600; text-transform: uppercase; letter-spacing: .06em; margin-bottom: 4px;">
                    Nouvelle mission détectée
                  </div>
                  <div style="font-size: 18px; font-weight: 700; color: #f1f5f9; line-height: 1.3;">
                    ${m.title}
                  </div>
                </div>
              </div>

              <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
                ${if (m.company != null) "<tr><td style='padding: 7px 0; border-bottom: 1px solid #1e293b; color: #94a3b8; font-size: 13px; width: 110px;'>Entreprise</td><td style='padding: 7px 0; border-bottom: 1px solid #334155; color: #f1f5f9; font-size: 13px; font-weight: 600;'>${m.company}</td></tr>" else ""}
                <tr><td style='padding: 7px 0; border-bottom: 1px solid #334155; color: #94a3b8; font-size: 13px;'>TJM</td><td style='padding: 7px 0; border-bottom: 1px solid #334155; color: #34d399; font-size: 13px; font-weight: 700;'>$tjm</td></tr>
                <tr><td style='padding: 7px 0; border-bottom: 1px solid #334155; color: #94a3b8; font-size: 13px;'>Télétravail</td><td style='padding: 7px 0; border-bottom: 1px solid #334155; color: #f1f5f9; font-size: 13px; font-weight: 600;'>${if (m.remote == true) "Oui" else "Non / Hybride"}</td></tr>
                ${if (m.duration != null) "<tr><td style='padding: 7px 0; border-bottom: 1px solid #334155; color: #94a3b8; font-size: 13px;'>Durée</td><td style='padding: 7px 0; border-bottom: 1px solid #334155; color: #f1f5f9; font-size: 13px; font-weight: 600;'>${m.duration}</td></tr>" else ""}
                <tr><td style='padding: 7px 0; color: #94a3b8; font-size: 13px;'>Source</td><td style='padding: 7px 0; color: #f1f5f9; font-size: 13px; font-weight: 600;'>${m.source?.name}</td></tr>
              </table>

              ${if (m.fitSummary != null) """
              <div style="background: #7c3aed22; border: 1px solid #7c3aed44; border-radius: 10px; padding: 14px; margin-bottom: 20px;">
                <div style="font-size: 11px; color: #a78bfa; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; margin-bottom: 8px;">Analyse Claude AI</div>
                <div style="font-size: 13px; color: #cbd5e1; line-height: 1.6;">${m.fitSummary}</div>
              </div>""" else ""}

              ${if (m.decisionMakerHint != null) """
              <div style="background: #059669_15; border: 1px solid #34d39944; border-radius: 10px; padding: 14px; margin-bottom: 20px; background: rgba(52,211,153,0.06);">
                <div style="font-size: 11px; color: #34d399; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; margin-bottom: 8px;">Stratégie de contact</div>
                <div style="font-size: 13px; color: #cbd5e1; line-height: 1.6;">${m.decisionMakerHint}</div>
              </div>""" else ""}

              <a href="$detailUrl" style="display: block; text-align: center; background: #7c3aed; color: #fff;
                 text-decoration: none; padding: 13px 24px; border-radius: 10px; font-weight: 700;
                 font-size: 14px; letter-spacing: .02em;">
                Voir la mission complète →
              </a>

            </div>
            <div style="text-align: center; margin-top: 16px; font-size: 11px; color: #475569;">
              Prospection Radar — alerte automatique score ≥ $threshold/100
            </div>
          </div>
        </body>
        </html>
        """.trimIndent()
    }
}
