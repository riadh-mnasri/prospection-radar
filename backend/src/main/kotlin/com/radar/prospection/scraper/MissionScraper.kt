package com.radar.prospection.scraper

import com.radar.prospection.domain.Mission
import com.radar.prospection.domain.Source

interface MissionScraper {
    fun getSource(): Source
    fun scrape(keywords: List<String>): List<Mission>
    fun supports(source: Source): Boolean = getSource() == source
}
