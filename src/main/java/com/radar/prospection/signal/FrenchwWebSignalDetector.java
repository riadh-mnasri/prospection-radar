package com.radar.prospection.signal;

import com.radar.prospection.config.RadarProperties;
import com.radar.prospection.domain.Signal;
import com.radar.prospection.domain.SignalType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Détecte les levées de fonds et nominations CTO sur les médias tech français.
 * Sources : Frenchweb, Maddyness, Les Echos Start
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrenchwWebSignalDetector implements SignalDetector {

    private final RadarProperties properties;

    private static final List<String> SOURCES = List.of(
        "https://www.frenchweb.fr/category/financement",
        "https://www.maddyness.com/category/financement",
        "https://www.frenchweb.fr/category/nominations"
    );

    private static final Pattern FUNDING_AMOUNT = Pattern.compile(
        "(\\d+(?:[,.]\\d+)?\\s*(?:millions?|M|K)\\s*(?:€|euros?|d'euros?))",
        Pattern.CASE_INSENSITIVE
    );

    private static final List<String> CTO_KEYWORDS = List.of(
        "CTO", "Chief Technology Officer", "VP Engineering",
        "Directeur Technique", "VP Tech", "Head of Engineering"
    );

    @Override
    public List<Signal> detect() {
        List<Signal> signals = new ArrayList<>();

        for (String sourceUrl : SOURCES) {
            try {
                List<Signal> found = scrapeSource(sourceUrl);
                signals.addAll(found);
                log.info("[FrenchwWeb] {} signaux depuis {}", found.size(), sourceUrl);
                Thread.sleep(properties.getScraping().getDelayBetweenRequestsMs());
            } catch (Exception e) {
                log.warn("[FrenchwWeb] Erreur {}: {}", sourceUrl, e.getMessage());
            }
        }

        return signals;
    }

    private List<Signal> scrapeSource(String url) throws Exception {
        List<Signal> signals = new ArrayList<>();

        Document doc = Jsoup.connect(url)
                .userAgent(properties.getScraping().getUserAgent())
                .timeout(15000)
                .get();

        Elements articles = doc.select("article, .post, .article-item, .entry");

        for (Element article : articles) {
            try {
                Signal signal = analyzeArticle(article, url);
                if (signal != null) signals.add(signal);
            } catch (Exception e) {
                log.debug("Erreur article: {}", e.getMessage());
            }
        }

        return signals;
    }

    private Signal analyzeArticle(Element article, String sourceUrl) {
        String title = extractText(article, "h2, h3, .entry-title, .post-title");
        String summary = extractText(article, ".excerpt, .summary, p");
        String articleUrl = extractUrl(article);

        if (title == null || title.isBlank()) return null;

        // Détecter levée de fonds
        if (isFundingArticle(title, summary)) {
            return buildFundingSignal(title, summary, articleUrl, sourceUrl);
        }

        // Détecter nomination CTO/VP Eng
        if (isNominationArticle(title, summary)) {
            return buildNominationSignal(title, summary, articleUrl, sourceUrl);
        }

        return null;
    }

    private boolean isFundingArticle(String title, String summary) {
        String text = (title + " " + (summary != null ? summary : "")).toLowerCase();
        return text.contains("lève") || text.contains("levée") || text.contains("financement")
                || text.contains("série a") || text.contains("série b") || text.contains("seed")
                || text.contains("tour de table") || text.contains("fundraising");
    }

    private boolean isNominationArticle(String title, String summary) {
        String text = (title + " " + (summary != null ? summary : "")).toLowerCase();
        boolean hasCtoKeyword = CTO_KEYWORDS.stream()
                .anyMatch(k -> text.contains(k.toLowerCase()));
        return hasCtoKeyword && (text.contains("nomme") || text.contains("recrute")
                || text.contains("arrive") || text.contains("rejoint") || text.contains("nouveau"));
    }

    private Signal buildFundingSignal(String title, String summary, String url, String sourceUrl) {
        String amount = extractFundingAmount(title + " " + summary);
        String company = extractCompanyName(title);

        return Signal.builder()
                .type(SignalType.FUNDING)
                .company(company)
                .title(title)
                .description(summary)
                .fundingAmount(amount)
                .url(url)
                .sourceWebsite(sourceUrl)
                .detectedAt(LocalDateTime.now())
                .build();
    }

    private Signal buildNominationSignal(String title, String summary, String url, String sourceUrl) {
        String company = extractCompanyName(title);
        String role = extractRole(title + " " + (summary != null ? summary : ""));

        return Signal.builder()
                .type(SignalType.CTO_NOMINATION)
                .company(company)
                .title(title)
                .description(summary)
                .newRole(role)
                .url(url)
                .sourceWebsite(sourceUrl)
                .detectedAt(LocalDateTime.now())
                .build();
    }

    private String extractFundingAmount(String text) {
        Matcher m = FUNDING_AMOUNT.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private String extractCompanyName(String title) {
        // Heuristique : souvent "NomSociété lève X millions" ou "NomSociété recrute un CTO"
        String[] words = title.split("\\s+");
        StringBuilder company = new StringBuilder();
        for (String word : words) {
            if (word.matches("[A-Z][a-zA-Z]+") || word.matches("[A-Z]{2,}")) {
                if (!company.isEmpty()) company.append(" ");
                company.append(word);
            } else if (!company.isEmpty()) {
                break;
            }
        }
        return company.isEmpty() ? title.split("\\s+")[0] : company.toString();
    }

    private String extractRole(String text) {
        for (String keyword : CTO_KEYWORDS) {
            if (text.toLowerCase().contains(keyword.toLowerCase())) {
                return keyword;
            }
        }
        return "Direction Technique";
    }

    private String extractText(Element container, String selector) {
        for (String sel : selector.split(",\\s*")) {
            Element el = container.selectFirst(sel.trim());
            if (el != null && !el.text().isBlank()) return el.text().trim();
        }
        return null;
    }

    private String extractUrl(Element article) {
        Element link = article.selectFirst("a[href]");
        return link != null ? link.absUrl("href") : null;
    }
}
