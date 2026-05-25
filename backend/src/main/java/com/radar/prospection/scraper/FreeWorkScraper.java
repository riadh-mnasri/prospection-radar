package com.radar.prospection.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radar.prospection.config.RadarProperties;
import com.radar.prospection.domain.Mission;
import com.radar.prospection.domain.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Scraper Free-Work (ex Freelance-info) via leur API JSON publique.
 * Endpoint: https://www.free-work.com/api/job_postings
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeWorkScraper implements MissionScraper {

    private final RadarProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    private static final String API_URL = "https://www.free-work.com/api/job_postings";
    private static final String BASE_URL = "https://www.free-work.com";

    @Override
    public Source getSource() {
        return Source.FREELANCE_COM;
    }

    @Override
    public List<Mission> scrape(List<String> keywords) {
        List<Mission> all = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (String keyword : keywords) {
            try {
                List<Mission> results = scrapeKeyword(keyword, seenIds);
                all.addAll(results);
                log.info("[Free-Work] '{}' → {} missions", keyword, results.size());
                Thread.sleep(properties.getScraping().getDelayBetweenRequestsMs());
            } catch (Exception e) {
                log.warn("[Free-Work] Erreur '{}': {}", keyword, e.getMessage());
            }
        }

        return all;
    }

    private List<Mission> scrapeKeyword(String keyword, Set<String> seenIds) throws Exception {
        List<Mission> missions = new ArrayList<>();

        for (int page = 1; page <= 3; page++) {
            String url = API_URL + "?page=" + page
                    + "&itemsPerPage=20"
                    + "&keywords=" + keyword.replace(" ", "+")
                    + "&contractTypes=freelance";

            String json = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", properties.getScraping().getUserAgent())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(json);

            // L'API retourne soit un tableau direct, soit un objet hydra:Collection
            JsonNode members = root.isArray() ? root : root.path("hydra:member");

            if (!members.isArray() || members.isEmpty()) break;

            for (JsonNode item : members) {
                Mission m = parseJob(item);
                if (m != null && seenIds.add(m.getExternalId())) {
                    missions.add(m);
                }
            }

            // Pas de page suivante si moins de 20 résultats
            if (members.size() < 20) break;

            Thread.sleep(properties.getScraping().getDelayBetweenRequestsMs());
        }

        return missions;
    }

    private Mission parseJob(JsonNode item) {
        try {
            String id = item.path("id").asText();
            String slug = item.path("slug").asText();
            String title = item.path("title").asText();
            if (title.isBlank()) return null;

            String description = cleanHtml(item.path("description").asText(null));
            String company = item.path("company").path("name").asText(null);
            String url = BASE_URL + "/fr/tech-it/job-mission/" + slug;

            // Location
            JsonNode loc = item.path("location");
            String city = loc.path("locality").asText(null);
            String country = loc.path("country").asText(null);
            String location = city != null ? city + (country != null ? ", " + country : "") : country;

            // TJM
            int tjmMin = item.path("minDailySalary").asInt(0);
            int tjmMax = item.path("maxDailySalary").asInt(0);

            // Durée
            String duration = null;
            if (!item.path("durationValue").isNull()) {
                duration = item.path("durationValue").asText() + " " + item.path("durationPeriod").asText("");
            }

            // Remote
            String remoteMode = item.path("remoteMode").asText("");
            boolean remote = remoteMode.toLowerCase().contains("remote")
                    || remoteMode.toLowerCase().contains("teletravail")
                    || remoteMode.equalsIgnoreCase("full");

            // Skills
            List<String> skills = new ArrayList<>();
            JsonNode skillsNode = item.path("skills");
            if (skillsNode.isArray()) {
                for (JsonNode skill : skillsNode) {
                    String skillName = skill.path("name").asText(null);
                    if (skillName != null) skills.add(skillName);
                }
            }

            // Date
            LocalDateTime publishedAt = null;
            String createdAt = item.path("createdAt").asText(null);
            if (createdAt != null) {
                try {
                    publishedAt = LocalDateTime.parse(createdAt.substring(0, 19));
                } catch (Exception ignored) {}
            }

            return Mission.builder()
                    .source(Source.FREELANCE_COM)
                    .externalId(id)
                    .title(title.trim())
                    .description(description)
                    .company(company)
                    .location(location)
                    .remote(remote)
                    .tjmMin(tjmMin > 0 ? tjmMin : null)
                    .tjmMax(tjmMax > 0 ? tjmMax : null)
                    .duration(duration)
                    .skills(skills)
                    .url(url)
                    .publishedAt(publishedAt)
                    .detectedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.debug("[Free-Work] Erreur parsing job: {}", e.getMessage());
            return null;
        }
    }

    private String cleanHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
}
