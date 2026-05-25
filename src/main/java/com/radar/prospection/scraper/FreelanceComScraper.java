package com.radar.prospection.scraper;

import com.radar.prospection.config.RadarProperties;
import com.radar.prospection.domain.Mission;
import com.radar.prospection.domain.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class FreelanceComScraper implements MissionScraper {

    private final RadarProperties properties;

    private static final String BASE_URL = "https://www.freelance.com/missions/";
    private static final Pattern TJM_PATTERN = Pattern.compile("(\\d+)\\s*[€$]?\\s*(?:[-–]\\s*(\\d+))?");
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)\\s*(mois|semaines?|jours?)");

    @Override
    public Source getSource() {
        return Source.FREELANCE_COM;
    }

    @Override
    public List<Mission> scrape(List<String> keywords) {
        List<Mission> all = new ArrayList<>();

        for (String keyword : keywords) {
            try {
                List<Mission> results = scrapeKeyword(keyword);
                all.addAll(results);
                log.info("[Freelance.com] '{}' → {} missions", keyword, results.size());
                Thread.sleep(properties.getScraping().getDelayBetweenRequestsMs());
            } catch (Exception e) {
                log.warn("[Freelance.com] Erreur '{}': {}", keyword, e.getMessage());
            }
        }

        return deduplicateByExternalId(all);
    }

    private List<Mission> scrapeKeyword(String keyword) throws IOException, InterruptedException {
        List<Mission> missions = new ArrayList<>();
        int page = 1;

        while (page <= 3) {
            String url = BASE_URL + "?term=" + keyword.replace(" ", "+")
                    + "&type=online&page=" + page;

            Document doc = Jsoup.connect(url)
                    .userAgent(properties.getScraping().getUserAgent())
                    .header("Accept-Language", "fr-FR,fr;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(15000)
                    .get();

            Elements cards = doc.select(".mission-card, .result-mission, article.mission, .list-item-mission");

            if (cards.isEmpty()) {
                // Fallback structure
                cards = doc.select("li.mission, div[class*=mission], div.result");
            }

            if (cards.isEmpty()) {
                log.debug("[Freelance.com] Aucune card page {}", page);
                break;
            }

            for (Element card : cards) {
                Mission mission = extractFromCard(card, keyword);
                if (mission != null) missions.add(mission);
            }

            // Vérifier s'il y a une page suivante
            boolean hasNext = !doc.select(".pagination .next, a[rel=next], a:contains(Suivant)").isEmpty();
            if (!hasNext) break;

            page++;
            Thread.sleep(properties.getScraping().getDelayBetweenRequestsMs());
        }

        return missions;
    }

    private Mission extractFromCard(Element card, String keyword) {
        try {
            String title = firstNonEmpty(card,
                "h2.mission-title", "h3", ".title", "a.mission-link", "h2");
            if (title == null || title.isBlank()) return null;

            // ID depuis l'attribut ou le lien
            String externalId = card.attr("data-mission-id");
            Element link = card.selectFirst("a[href*='/missions/'], a[href*='/mission/']");
            String url = null;
            if (link != null) {
                url = link.absUrl("href");
                if (externalId.isBlank()) {
                    externalId = extractIdFromUrl(url);
                }
            }
            if (externalId == null || externalId.isBlank()) {
                externalId = String.valueOf(Math.abs(title.hashCode()));
            }

            String description = firstNonEmpty(card, ".mission-description", ".description", "p");
            String company = firstNonEmpty(card, ".company", ".client", ".enterprise");
            String location = firstNonEmpty(card, ".location", ".city", "[class*=location]");
            String durationText = firstNonEmpty(card, ".duration", "[class*=duration]");
            String tjmText = firstNonEmpty(card, ".tjm", ".budget", ".rate", "[class*=tjm]", "[class*=budget]");

            int[] tjm = parseTjm(tjmText);
            String duration = parseDuration(durationText);
            boolean remote = card.text().toLowerCase().contains("télétravail")
                    || card.text().toLowerCase().contains("remote")
                    || card.text().toLowerCase().contains("full remote");

            List<String> skills = extractSkills(card, keyword);

            return Mission.builder()
                    .source(Source.FREELANCE_COM)
                    .externalId(externalId)
                    .title(title.trim())
                    .description(description)
                    .company(company)
                    .location(location)
                    .remote(remote)
                    .tjmMin(tjm[0])
                    .tjmMax(tjm[1])
                    .duration(duration)
                    .skills(skills)
                    .url(url)
                    .detectedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.debug("[Freelance.com] Extraction échouée: {}", e.getMessage());
            return null;
        }
    }

    private String firstNonEmpty(Element container, String... selectors) {
        for (String selector : selectors) {
            Element el = container.selectFirst(selector);
            if (el != null && !el.text().isBlank()) {
                return el.text().trim();
            }
        }
        return null;
    }

    private List<String> extractSkills(Element card, String keyword) {
        List<String> skills = new ArrayList<>();
        Elements tags = card.select(".tag, .skill, .technology, [class*=skill], [class*=tag]");
        for (Element tag : tags) {
            String text = tag.text().trim();
            if (!text.isBlank() && text.length() < 50) skills.add(text);
        }
        if (skills.isEmpty() && keyword != null) {
            skills = List.of(keyword.split("\\s+"));
        }
        return skills;
    }

    private int[] parseTjm(String text) {
        int[] result = {0, 0};
        if (text == null) return result;
        Matcher m = TJM_PATTERN.matcher(text.replaceAll("\\s+", ""));
        if (m.find()) {
            result[0] = Integer.parseInt(m.group(1));
            result[1] = m.group(2) != null ? Integer.parseInt(m.group(2)) : result[0];
        }
        return result;
    }

    private String parseDuration(String text) {
        if (text == null) return null;
        Matcher m = DURATION_PATTERN.matcher(text.toLowerCase());
        if (m.find()) return m.group(1) + " " + m.group(2);
        return text.trim().isEmpty() ? null : text.trim();
    }

    private String extractIdFromUrl(String url) {
        if (url == null) return null;
        String[] parts = url.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("\\d+")) return parts[i];
        }
        return null;
    }

    private List<Mission> deduplicateByExternalId(List<Mission> missions) {
        Map<String, Mission> unique = new LinkedHashMap<>();
        for (Mission m : missions) {
            unique.putIfAbsent(m.getExternalId(), m);
        }
        return new ArrayList<>(unique.values());
    }
}
