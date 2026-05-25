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

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraper Hellowork — accessible sans login, offres freelance/portage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HelloworkScraper implements MissionScraper {

    private final RadarProperties properties;

    private static final String SEARCH_URL = "https://www.hellowork.com/fr-fr/emploi/recherche.html";
    private static final Pattern TJM_PATTERN = Pattern.compile("(\\d{3,4})\\s*[€$]?\\s*(?:[-–]\\s*(\\d{3,4}))?");

    @Override
    public Source getSource() {
        return Source.MALT;
    }

    @Override
    public List<Mission> scrape(List<String> keywords) {
        List<Mission> all = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (String keyword : keywords) {
            try {
                List<Mission> found = scrapeKeyword(keyword, seenIds);
                all.addAll(found);
                log.info("[Hellowork] '{}' → {} missions", keyword, found.size());
                Thread.sleep(properties.getScraping().getDelayBetweenRequestsMs());
            } catch (Exception e) {
                log.warn("[Hellowork] Erreur '{}': {}", keyword, e.getMessage());
            }
        }
        return all;
    }

    private List<Mission> scrapeKeyword(String keyword, Set<String> seenIds) throws Exception {
        List<Mission> missions = new ArrayList<>();

        String url = SEARCH_URL + "?k=" + keyword.replace(" ", "+")
                + "&c=Freelance%2FPortage&p=france&ray=national";

        Document doc = Jsoup.connect(url)
                .userAgent(properties.getScraping().getUserAgent())
                .header("Accept-Language", "fr-FR,fr;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml")
                .timeout(15000)
                .followRedirects(true)
                .get();

        // Chercher les cards d'offres
        Elements cards = doc.select("li[data-id], article[data-id], [class*='job-item'], [class*='offer-item']");
        if (cards.isEmpty()) {
            cards = doc.select("li.is-job, [data-cy='jobCard'], [data-test='job-card']");
        }
        if (cards.isEmpty()) {
            // Fallback générique
            cards = doc.select("article, li").stream()
                    .filter(e -> e.selectFirst("a[href*='/offre-']") != null
                            || e.selectFirst("a[href*='/emploi/']") != null)
                    .collect(java.util.stream.Collectors.toCollection(Elements::new));
        }

        log.debug("[Hellowork] {} cards trouvées pour '{}'", cards.size(), keyword);

        for (Element card : cards) {
            Mission m = parseCard(card, keyword);
            if (m != null && seenIds.add(m.getExternalId())) {
                missions.add(m);
            }
        }

        return missions;
    }

    private Mission parseCard(Element card, String keyword) {
        try {
            String title = firstText(card, "h2, h3, [class*='title'], [class*='job-name']");
            if (title == null || title.isBlank()) return null;

            // ID depuis data-id ou URL
            String externalId = card.attr("data-id");
            Element link = card.selectFirst("a[href]");
            String url = null;
            if (link != null) {
                url = link.absUrl("href");
                if (externalId.isBlank()) externalId = extractIdFromUrl(url);
            }
            if (externalId == null || externalId.isBlank()) {
                externalId = String.valueOf(Math.abs(title.hashCode()));
            }

            String company = firstText(card, "[class*='company'], [class*='employer'], [itemprop='name']");
            String location = firstText(card, "[class*='location'], [class*='city'], [itemprop='addressLocality']");
            String salaryText = firstText(card, "[class*='salary'], [class*='wage'], [class*='tjm'], [class*='remuneration']");

            boolean remote = card.text().toLowerCase().contains("télétravail")
                    || card.text().toLowerCase().contains("remote");

            int[] tjm = parseTjm(salaryText);
            List<String> skills = extractSkills(card, keyword);

            return Mission.builder()
                    .source(Source.MALT)
                    .externalId(externalId)
                    .title(title.trim())
                    .company(company)
                    .location(location)
                    .remote(remote)
                    .tjmMin(tjm[0] > 0 ? tjm[0] : null)
                    .tjmMax(tjm[1] > 0 ? tjm[1] : null)
                    .skills(skills)
                    .url(url)
                    .detectedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.debug("[Hellowork] Erreur parsing card: {}", e.getMessage());
            return null;
        }
    }

    private String firstText(Element container, String selector) {
        for (String sel : selector.split(",\\s*")) {
            Element el = container.selectFirst(sel.trim());
            if (el != null && !el.text().isBlank()) return el.text().trim();
        }
        return null;
    }

    private List<String> extractSkills(Element card, String keyword) {
        List<String> skills = new ArrayList<>();
        Elements tags = card.select("[class*='tag'], [class*='skill'], [class*='keyword']");
        for (Element tag : tags) {
            String t = tag.text().trim();
            if (!t.isBlank() && t.length() < 40) skills.add(t);
        }
        if (skills.isEmpty() && keyword != null) {
            skills = Arrays.asList(keyword.split("\\s+"));
        }
        return skills;
    }

    private int[] parseTjm(String text) {
        int[] r = {0, 0};
        if (text == null) return r;
        Matcher m = TJM_PATTERN.matcher(text.replaceAll("\\s", ""));
        if (m.find()) {
            r[0] = Integer.parseInt(m.group(1));
            r[1] = m.group(2) != null ? Integer.parseInt(m.group(2)) : r[0];
        }
        return r;
    }

    private String extractIdFromUrl(String url) {
        if (url == null) return null;
        Matcher m = Pattern.compile("/(\\d{6,})").matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
