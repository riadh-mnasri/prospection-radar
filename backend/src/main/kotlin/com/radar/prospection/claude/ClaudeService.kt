package com.radar.prospection.claude

import com.fasterxml.jackson.databind.ObjectMapper
import com.radar.prospection.config.RadarProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class ClaudeService(
    private val properties: RadarProperties,
    private val webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun buildClient(): WebClient = webClientBuilder
        .baseUrl(properties.claude.baseUrl)
        .defaultHeader("x-api-key", properties.claude.apiKey)
        .defaultHeader("anthropic-version", "2023-06-01")
        .defaultHeader("content-type", "application/json")
        .build()

    fun complete(systemPrompt: String, userMessage: String): String {
        try {
            val body = mapOf(
                "model" to properties.claude.model,
                "max_tokens" to 1024,
                "system" to systemPrompt,
                "messages" to listOf(mapOf("role" to "user", "content" to userMessage))
            )

            val response = buildClient().post()
                .uri("/v1/messages")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()!!

            val json = objectMapper.readTree(response)
            return json.path("content").get(0).path("text").asText()

        } catch (e: Exception) {
            log.error("Erreur appel Claude: {}", e.message)
            throw RuntimeException("Claude API error: ${e.message}", e)
        }
    }

    fun <T> completeAsJson(systemPrompt: String, userMessage: String, type: Class<T>): T {
        val raw = complete(systemPrompt, userMessage)
        try {
            return objectMapper.readValue(extractJson(raw), type)
        } catch (e: Exception) {
            log.error("Erreur parsing JSON Claude: {}\nRéponse brute: {}", e.message, raw)
            throw RuntimeException("Failed to parse Claude JSON response", e)
        }
    }

    private fun extractJson(text: String): String {
        var start = text.indexOf('{')
        var end = text.lastIndexOf('}')
        if (start >= 0 && end > start) return text.substring(start, end + 1)
        start = text.indexOf('[')
        end = text.lastIndexOf(']')
        if (start >= 0 && end > start) return text.substring(start, end + 1)
        return text
    }
}
