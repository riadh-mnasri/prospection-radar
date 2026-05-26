package com.radar.prospection.scraper

import com.radar.prospection.config.RadarProperties
import com.radar.prospection.domain.Mission
import com.radar.prospection.domain.Source
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.regex.Pattern

@Component
class HelloworkScraper(private val properties: RadarProperties) : MissionScraper {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val SEARCH_URL = "https://www.hellowork.com/fr-fr/emploi/recherche.html"
        private val TJM_PATTERN = Pattern.compile("(\\d{3,4})\\s*[€\$]?\\s*(?:[-–]\\s*(\\d{3,4}))?")
    }

    override fun getSource() = Source.MALT

    override fun scrape(keywords: List<String>): List<Mission> {
        val all = mutableListOf<Mission>()
        val seenIds = mutableSetOf<String>()

        for (keyword in keywords) {
            try {
                val found = scrapeKeyword(keyword, seenIds)
                all.addAll(found)
                log.info("[Hellowork] '{}' → {} missions", keyword, found.size)
                Thread.sleep(properties.scraping.delayBetweenRequestsMs)
            } catch (e: Exception) {
                log.warn("[Hellowork] Erreur '{}': {}", keyword, e.message)
            }
        }
        return all
    }

    private fun scrapeKeyword(keyword: String, seenIds: MutableSet<String>): List<Mission> {
        val missions = mutableListOf<Mission>()
        val url = "$SEARCH_URL?k=${keyword.replace(" ", "+")}&c=Freelance%2FPortage&p=france&ray=national"

        val doc = Jsoup.connect(url)
            .userAgent(properties.scraping.userAgent)
            .header("Accept-Language", "fr-FR,fr;q=0.9")
            .header("Accept", "text/html,application/xhtml+xml")
            .timeout(15000)
            .followRedirects(true)
            .get()

        var cards = doc.select("li[data-id], article[data-id], [class*='job-item'], [class*='offer-item']")
        if (cards.isEmpty()) cards = doc.select("li.is-job, [data-cy='jobCard'], [data-test='job-card']")
        if (cards.isEmpty()) {
            cards = doc.select("article, li")
                .filter { e -> e.selectFirst("a[href*='/offre-']") != null || e.selectFirst("a[href*='/emploi/']") != null }
                .let { Elements(it) }
        }

        log.debug("[Hellowork] {} cards trouvées pour '{}'", cards.size, keyword)

        for (card in cards) {
            val m = parseCard(card, keyword) ?: continue
            if (seenIds.add(m.externalId ?: continue)) missions.add(m)
        }

        return missions
    }

    private fun parseCard(card: Element, keyword: String): Mission? {
        return try {
            val title = firstText(card, "h2, h3, [class*='title'], [class*='job-name']") ?: return null
            if (title.isBlank()) return null

            var externalId = card.attr("data-id")
            val link = card.selectFirst("a[href]")
            val url = link?.absUrl("href")
            if (externalId.isBlank()) externalId = url?.let { extractIdFromUrl(it) } ?: ""
            if (externalId.isBlank()) externalId = title.hashCode().toLong().toString()

            val company = firstText(card, "[class*='company'], [class*='employer'], [itemprop='name']")
            val location = firstText(card, "[class*='location'], [class*='city'], [itemprop='addressLocality']")
            val salaryText = firstText(card, "[class*='salary'], [class*='wage'], [class*='tjm'], [class*='remuneration']")

            val remote = card.text().lowercase().contains("télétravail") || card.text().lowercase().contains("remote")
            val tjm = parseTjm(salaryText)
            val skills = extractSkills(card, keyword)

            Mission(
                source = Source.MALT,
                externalId = externalId,
                title = title.trim(),
                company = company,
                location = location,
                remote = remote,
                tjmMin = if (tjm[0] > 0) tjm[0] else null,
                tjmMax = if (tjm[1] > 0) tjm[1] else null,
                skills = skills,
                url = url,
                detectedAt = LocalDateTime.now()
            )
        } catch (e: Exception) {
            log.debug("[Hellowork] Erreur parsing card: {}", e.message)
            null
        }
    }

    private fun firstText(container: Element, selector: String): String? {
        for (sel in selector.split(",\\s*".toRegex())) {
            val el = container.selectFirst(sel.trim())
            if (el != null && !el.text().isBlank()) return el.text().trim()
        }
        return null
    }

    private fun extractSkills(card: Element, keyword: String): MutableList<String> {
        val tags = card.select("[class*='tag'], [class*='skill'], [class*='keyword']")
        val skills = tags.map { it.text().trim() }.filter { it.isNotBlank() && it.length < 40 }.toMutableList()
        if (skills.isEmpty()) return keyword.split("\\s+".toRegex()).toMutableList()
        return skills
    }

    private fun parseTjm(text: String?): IntArray {
        val r = intArrayOf(0, 0)
        if (text == null) return r
        val m = TJM_PATTERN.matcher(text.replace("\\s".toRegex(), ""))
        if (m.find()) {
            r[0] = m.group(1).toInt()
            r[1] = if (m.group(2) != null) m.group(2).toInt() else r[0]
        }
        return r
    }

    private fun extractIdFromUrl(url: String): String? {
        val m = Pattern.compile("/(\\d{6,})").matcher(url)
        return if (m.find()) m.group(1) else null
    }
}
