package com.radar.prospection.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "radar")
class RadarProperties {
    val claude = Claude()
    val profile = Profile()
    val scraping = Scraping()
    val scheduling = Scheduling()

    class Claude {
        var apiKey: String = ""
        var model: String = "claude-sonnet-4-6"
        var baseUrl: String = "https://api.anthropic.com"
    }

    class Profile {
        var skills: List<String> = emptyList()
        var tjmMin: Int = 0
        var tjmMax: Int = 0
        var remotePreference: String = ""
        var location: String = ""
    }

    class Scraping {
        var delayBetweenRequestsMs: Long = 2000
        var maxRetries: Int = 3
        var userAgent: String = ""
        var keywords: List<String> = emptyList()
    }

    class Scheduling {
        var jobBoardsCron: String = ""
        var signalsCron: String = ""
    }
}
