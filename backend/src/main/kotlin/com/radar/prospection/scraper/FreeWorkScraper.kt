package com.radar.prospection.scraper

import com.fasterxml.jackson.databind.ObjectMapper
import com.radar.prospection.config.RadarProperties
import com.radar.prospection.domain.Mission
import com.radar.prospection.domain.Source
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime

@Component
class FreeWorkScraper(
    private val properties: RadarProperties,
    private val objectMapper: ObjectMapper,
    private val webClientBuilder: WebClient.Builder
) : MissionScraper {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val API_URL = "https://www.free-work.com/api/job_postings"
        private const val BASE_URL = "https://www.free-work.com"
    }

    override fun getSource() = Source.FREELANCE_COM

    override fun scrape(keywords: List<String>): List<Mission> {
        val all = mutableListOf<Mission>()
        val seenIds = mutableSetOf<String>()

        for (keyword in keywords) {
            try {
                val results = scrapeKeyword(keyword, seenIds)
                all.addAll(results)
                log.info("[Free-Work] '{}' → {} missions", keyword, results.size)
                Thread.sleep(properties.scraping.delayBetweenRequestsMs)
            } catch (e: Exception) {
                log.warn("[Free-Work] Erreur '{}': {}", keyword, e.message)
            }
        }
        return all
    }

    private fun scrapeKeyword(keyword: String, seenIds: MutableSet<String>): List<Mission> {
        val missions = mutableListOf<Mission>()

        for (page in 1..3) {
            val url = "$API_URL?page=$page&itemsPerPage=20&keywords=${keyword.replace(" ", "+")}&contractTypes=freelance"

            val json = webClientBuilder.build().get()
                .uri(url)
                .header("Accept", "application/json")
                .header("User-Agent", properties.scraping.userAgent)
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: break

            val root = objectMapper.readTree(json)
            val members = if (root.isArray) root else root.path("hydra:member")
            if (!members.isArray || members.isEmpty) break

            for (item in members) {
                val m = parseJob(item) ?: continue
                if (seenIds.add(m.externalId ?: continue)) missions.add(m)
            }

            if (members.size() < 20) break
            Thread.sleep(properties.scraping.delayBetweenRequestsMs)
        }

        return missions
    }

    private fun parseJob(item: com.fasterxml.jackson.databind.JsonNode): Mission? {
        return try {
            val id = item.path("id").asText()
            val slug = item.path("slug").asText()
            val title = item.path("title").asText()
            if (title.isBlank()) return null

            val description = cleanHtml(item.path("description").asText(null))
            val company = item.path("company").path("name").asText(null)
            val url = "$BASE_URL/fr/tech-it/job-mission/$slug"

            val loc = item.path("location")
            val city = loc.path("locality").asText(null)
            val country = loc.path("country").asText(null)
            val location = when {
                city != null -> city + (if (country != null) ", $country" else "")
                else -> country
            }

            val tjmMin = item.path("minDailySalary").asInt(0)
            val tjmMax = item.path("maxDailySalary").asInt(0)

            val duration = if (!item.path("durationValue").isNull)
                "${item.path("durationValue").asText()} ${item.path("durationPeriod").asText("")}"
            else null

            val remoteMode = item.path("remoteMode").asText("")
            val remote = remoteMode.lowercase().contains("remote")
                    || remoteMode.lowercase().contains("teletravail")
                    || remoteMode.equals("full", ignoreCase = true)

            val skills = mutableListOf<String>()
            val skillsNode = item.path("skills")
            if (skillsNode.isArray) {
                for (skill in skillsNode) {
                    skill.path("name").asText(null)?.let { skills.add(it) }
                }
            }

            val publishedAt = item.path("createdAt").asText(null)?.let {
                try { LocalDateTime.parse(it.take(19)) } catch (_: Exception) { null }
            }

            Mission(
                source = Source.FREELANCE_COM,
                externalId = id,
                title = title.trim(),
                description = description,
                company = company,
                location = location,
                remote = remote,
                tjmMin = if (tjmMin > 0) tjmMin else null,
                tjmMax = if (tjmMax > 0) tjmMax else null,
                duration = duration,
                skills = skills,
                url = url,
                publishedAt = publishedAt,
                detectedAt = LocalDateTime.now()
            )
        } catch (e: Exception) {
            log.debug("[Free-Work] Erreur parsing job: {}", e.message)
            null
        }
    }

    private fun cleanHtml(html: String?): String? =
        html?.replace(Regex("<[^>]+>"), " ")?.replace(Regex("\\s+"), " ")?.trim()
}
