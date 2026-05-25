package com.radar.prospection.scraper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.radar.prospection.config.RadarProperties;
import com.radar.prospection.domain.Mission;
import com.radar.prospection.domain.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaltScraper implements MissionScraper {

    private final RadarProperties properties;

    private static final String BASE_URL = "https://www.malt.fr/missions";
    private static final Pattern TJM_PATTERN = Pattern.compile("(\\d+)\\s*[€$]?\\s*(?:[-–]\\s*(\\d+)\\s*[€$]?)?\\s*/\\s*(?:jour|day|j)");

    @Override
    public Source getSource() {
        return Source.MALT;
    }

    @Override
    public List<Mission> scrape(List<String> keywords) {
        List<Mission> missions = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--no-sandbox", "--disable-blink-features=AutomationControlled"));

            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                        .setUserAgent(properties.getScraping().getUserAgent())
                        .setViewportSize(1920, 1080)
                        .setLocale("fr-FR"));

                for (String keyword : keywords) {
                    try {
                        List<Mission> keywordMissions = scrapeKeyword(context, keyword);
                        missions.addAll(keywordMissions);
                        log.info("[Malt] '{}' → {} missions trouvées", keyword, keywordMissions.size());
                        Thread.sleep(properties.getScraping().getDelayBetweenRequestsMs());
                    } catch (Exception e) {
                        log.warn("[Malt] Erreur pour keyword '{}': {}", keyword, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Malt] Erreur critique: {}", e.getMessage());
        }

        return deduplicateByExternalId(missions);
    }

    private List<Mission> scrapeKeyword(BrowserContext context, String keyword) throws InterruptedException {
        List<Mission> missions = new ArrayList<>();
        String url = BASE_URL + "?q=" + keyword.replace(" ", "+") + "&remote=true";

        try (Page page = context.newPage()) {
            page.setExtraHTTPHeaders(Map.of(
                "Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8",
                "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
            ));

            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            Thread.sleep(2000);

            // Fermer la bannière cookies si présente
            dismissCookieBanner(page);

            // Scraper jusqu'à 3 pages
            for (int pageNum = 1; pageNum <= 3; pageNum++) {
                List<Mission> pageMissions = extractMissionsFromPage(page, keyword);
                missions.addAll(pageMissions);

                if (!goToNextPage(page)) break;
                Thread.sleep(properties.getScraping().getDelayBetweenRequestsMs());
            }
        }

        return missions;
    }

    private List<Mission> extractMissionsFromPage(Page page, String keyword) {
        List<Mission> missions = new ArrayList<>();

        // Sélecteurs Malt (adaptés à la structure réelle)
        List<ElementHandle> cards = page.querySelectorAll("[data-testid='mission-card'], .mission-card, article.mission");

        if (cards.isEmpty()) {
            // Fallback : chercher par structure générique
            cards = page.querySelectorAll("article, .result-item, [class*='mission']");
        }

        log.debug("[Malt] {} cards trouvées sur la page", cards.size());

        for (ElementHandle card : cards) {
            try {
                Mission mission = extractMissionFromCard(card, keyword);
                if (mission != null) {
                    missions.add(mission);
                }
            } catch (Exception e) {
                log.debug("[Malt] Erreur extraction card: {}", e.getMessage());
            }
        }

        return missions;
    }

    private Mission extractMissionFromCard(ElementHandle card, String keyword) {
        String title = extractText(card, "h2, h3, [data-testid='mission-title'], .mission-title");
        if (title == null || title.isBlank()) return null;

        String externalId = card.getAttribute("data-mission-id");
        if (externalId == null) {
            externalId = String.valueOf(Math.abs(title.hashCode()));
        }

        String description = extractText(card, "[data-testid='mission-description'], .mission-description, p");
        String company = extractText(card, "[data-testid='company-name'], .company-name, .client-name");
        String duration = extractText(card, "[data-testid='duration'], .duration, [class*='duration']");
        String location = extractText(card, "[data-testid='location'], .location, [class*='location']");

        String tjmText = extractText(card, "[data-testid='tjm'], .tjm, [class*='budget'], [class*='tjm']");
        int[] tjm = parseTjm(tjmText);

        String cardHref = null;
        ElementHandle link = card.querySelector("a[href*='/mission/'], a[href*='/missions/']");
        if (link != null) {
            cardHref = link.getAttribute("href");
            if (cardHref != null && !cardHref.startsWith("http")) {
                cardHref = "https://www.malt.fr" + cardHref;
            }
        }

        List<String> skills = extractSkills(card);
        if (skills.isEmpty() && keyword != null) {
            skills = List.of(keyword.split(" "));
        }

        boolean isRemote = location != null && location.toLowerCase().contains("télétravail")
                || Boolean.TRUE.equals(card.evaluate("el => el.innerText").toString().toLowerCase().contains("télétravail"));

        return Mission.builder()
                .source(Source.MALT)
                .externalId(externalId)
                .title(title.trim())
                .description(description)
                .company(company)
                .location(location)
                .remote(isRemote)
                .tjmMin(tjm[0])
                .tjmMax(tjm[1])
                .duration(duration)
                .skills(skills)
                .url(cardHref)
                .detectedAt(LocalDateTime.now())
                .build();
    }

    private String extractText(ElementHandle container, String selector) {
        for (String sel : selector.split(",\\s*")) {
            try {
                ElementHandle el = container.querySelector(sel.trim());
                if (el != null) {
                    String text = el.innerText();
                    if (text != null && !text.isBlank()) {
                        return text.trim();
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private List<String> extractSkills(ElementHandle card) {
        List<String> skills = new ArrayList<>();
        List<ElementHandle> skillEls = card.querySelectorAll(
            "[data-testid='skill'], .skill-tag, .skill, [class*='tag'], [class*='skill']"
        );
        for (ElementHandle el : skillEls) {
            try {
                String text = el.innerText();
                if (text != null && !text.isBlank() && text.length() < 50) {
                    skills.add(text.trim());
                }
            } catch (Exception ignored) {}
        }
        return skills;
    }

    private int[] parseTjm(String text) {
        int[] result = {0, 0};
        if (text == null) return result;
        Matcher m = TJM_PATTERN.matcher(text);
        if (m.find()) {
            result[0] = Integer.parseInt(m.group(1));
            result[1] = m.group(2) != null ? Integer.parseInt(m.group(2)) : result[0];
        }
        return result;
    }

    private boolean goToNextPage(Page page) {
        try {
            ElementHandle nextBtn = page.querySelector("[aria-label='Page suivante'], [data-testid='next-page'], button:has-text('Suivant')");
            if (nextBtn != null && nextBtn.isEnabled()) {
                nextBtn.click();
                page.waitForLoadState();
                Thread.sleep(1500);
                return true;
            }
        } catch (Exception e) {
            log.debug("[Malt] Pas de page suivante: {}", e.getMessage());
        }
        return false;
    }

    private void dismissCookieBanner(Page page) {
        try {
            ElementHandle acceptBtn = page.querySelector(
                "button:has-text('Accepter'), button:has-text('Accept'), [id*='accept-cookies'], #didomi-notice-agree-button"
            );
            if (acceptBtn != null) {
                acceptBtn.click();
                Thread.sleep(500);
            }
        } catch (Exception ignored) {}
    }

    private List<Mission> deduplicateByExternalId(List<Mission> missions) {
        Map<String, Mission> unique = new LinkedHashMap<>();
        for (Mission m : missions) {
            unique.putIfAbsent(m.getSource() + "_" + m.getExternalId(), m);
        }
        return new ArrayList<>(unique.values());
    }
}
