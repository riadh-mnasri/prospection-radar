package com.radar.prospection.signal

import com.radar.prospection.config.RadarProperties
import com.radar.prospection.domain.Signal
import com.radar.prospection.domain.SignalType
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.regex.Pattern

@Component
class FrenchwWebSignalDetector(private val properties: RadarProperties) : SignalDetector {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val SOURCES = listOf(
            "https://www.frenchweb.fr/category/financement",
            "https://www.maddyness.com/category/financement",
            "https://www.frenchweb.fr/category/nominations"
        )

        private val FUNDING_AMOUNT = Pattern.compile(
            "(\\d+(?:[,.]\\d+)?\\s*(?:millions?|M|K)\\s*(?:€|euros?|d'euros?))",
            Pattern.CASE_INSENSITIVE
        )

        private val CTO_KEYWORDS = listOf(
            "CTO", "Chief Technology Officer", "VP Engineering",
            "Directeur Technique", "VP Tech", "Head of Engineering"
        )
    }

    override fun detect(): List<Signal> {
        val signals = mutableListOf<Signal>()

        for (sourceUrl in SOURCES) {
            try {
                val found = scrapeSource(sourceUrl)
                signals.addAll(found)
                log.info("[FrenchwWeb] {} signaux depuis {}", found.size, sourceUrl)
                Thread.sleep(properties.scraping.delayBetweenRequestsMs)
            } catch (e: Exception) {
                log.warn("[FrenchwWeb] Erreur {}: {}", sourceUrl, e.message)
            }
        }

        return signals
    }

    private fun scrapeSource(url: String): List<Signal> {
        val signals = mutableListOf<Signal>()

        val doc = Jsoup.connect(url)
            .userAgent(properties.scraping.userAgent)
            .timeout(15000)
            .get()

        for (article in doc.select("article, .post, .article-item, .entry")) {
            try {
                analyzeArticle(article, url)?.let { signals.add(it) }
            } catch (e: Exception) {
                log.debug("Erreur article: {}", e.message)
            }
        }

        return signals
    }

    private fun analyzeArticle(article: Element, sourceUrl: String): Signal? {
        val title = extractText(article, "h2, h3, .entry-title, .post-title") ?: return null
        if (title.isBlank()) return null
        val summary = extractText(article, ".excerpt, .summary, p")
        val articleUrl = extractUrl(article)

        return when {
            isFundingArticle(title, summary) -> buildFundingSignal(title, summary, articleUrl, sourceUrl)
            isNominationArticle(title, summary) -> buildNominationSignal(title, summary, articleUrl, sourceUrl)
            else -> null
        }
    }

    private fun isFundingArticle(title: String, summary: String?): Boolean {
        val text = "$title ${summary ?: ""}".lowercase()
        return text.contains("lève") || text.contains("levée") || text.contains("financement")
                || text.contains("série a") || text.contains("série b") || text.contains("seed")
                || text.contains("tour de table") || text.contains("fundraising")
    }

    private fun isNominationArticle(title: String, summary: String?): Boolean {
        val text = "$title ${summary ?: ""}".lowercase()
        val hasCtoKeyword = CTO_KEYWORDS.any { text.contains(it.lowercase()) }
        return hasCtoKeyword && (text.contains("nomme") || text.contains("recrute")
                || text.contains("arrive") || text.contains("rejoint") || text.contains("nouveau"))
    }

    private fun buildFundingSignal(title: String, summary: String?, url: String?, sourceUrl: String): Signal {
        val amount = extractFundingAmount("$title $summary")
        val company = extractCompanyName(title)
        return Signal(
            type = SignalType.FUNDING,
            company = company,
            title = title,
            description = summary,
            fundingAmount = amount,
            url = url,
            sourceWebsite = sourceUrl,
            detectedAt = LocalDateTime.now()
        )
    }

    private fun buildNominationSignal(title: String, summary: String?, url: String?, sourceUrl: String): Signal {
        val company = extractCompanyName(title)
        val role = extractRole("$title ${summary ?: ""}")
        return Signal(
            type = SignalType.CTO_NOMINATION,
            company = company,
            title = title,
            description = summary,
            newRole = role,
            url = url,
            sourceWebsite = sourceUrl,
            detectedAt = LocalDateTime.now()
        )
    }

    private fun extractFundingAmount(text: String): String? {
        val m = FUNDING_AMOUNT.matcher(text)
        return if (m.find()) m.group(1) else null
    }

    private fun extractCompanyName(title: String): String {
        val sb = StringBuilder()
        for (word in title.split("\\s+".toRegex())) {
            if (word.matches("[A-Z][a-zA-Z]+".toRegex()) || word.matches("[A-Z]{2,}".toRegex())) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(word)
            } else if (sb.isNotEmpty()) break
        }
        return if (sb.isEmpty()) title.split("\\s+".toRegex()).first() else sb.toString()
    }

    private fun extractRole(text: String): String {
        return CTO_KEYWORDS.firstOrNull { text.lowercase().contains(it.lowercase()) } ?: "Direction Technique"
    }

    private fun extractText(container: Element, selector: String): String? {
        for (sel in selector.split(",\\s*".toRegex())) {
            val el = container.selectFirst(sel.trim())
            if (el != null && !el.text().isBlank()) return el.text().trim()
        }
        return null
    }

    private fun extractUrl(article: Element): String? = article.selectFirst("a[href]")?.absUrl("href")
}
