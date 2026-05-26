package com.radar.prospection

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ProspectionRadarApplication

fun main(args: Array<String>) {
    runApplication<ProspectionRadarApplication>(*args)
}
