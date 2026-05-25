package com.radar.prospection.scraper;

import com.radar.prospection.domain.Mission;
import com.radar.prospection.domain.Source;

import java.util.List;

public interface MissionScraper {

    Source getSource();

    List<Mission> scrape(List<String> keywords);

    default boolean supports(Source source) {
        return getSource() == source;
    }
}
