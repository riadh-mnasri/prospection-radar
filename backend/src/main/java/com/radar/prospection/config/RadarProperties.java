package com.radar.prospection.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "radar")
@Data
public class RadarProperties {

    private Claude claude = new Claude();
    private Profile profile = new Profile();
    private Scraping scraping = new Scraping();
    private Scheduling scheduling = new Scheduling();

    @Data
    public static class Claude {
        private String apiKey;
        private String model = "claude-sonnet-4-6";
        private String baseUrl = "https://api.anthropic.com";
    }

    @Data
    public static class Profile {
        private List<String> skills;
        private int tjmMin;
        private int tjmMax;
        private String remotePreference;
        private String location;
    }

    @Data
    public static class Scraping {
        private long delayBetweenRequestsMs = 2000;
        private int maxRetries = 3;
        private String userAgent;
        private List<String> keywords;
    }

    @Data
    public static class Scheduling {
        private String jobBoardsCron;
        private String signalsCron;
    }
}
