package com.radar.prospection.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.radar.prospection.config.RadarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    private final RadarProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private WebClient buildClient() {
        return webClientBuilder
                .baseUrl(properties.getClaude().getBaseUrl())
                .defaultHeader("x-api-key", properties.getClaude().getApiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    public String complete(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> body = Map.of(
                "model", properties.getClaude().getModel(),
                "max_tokens", 1024,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
            );

            String response = buildClient().post()
                    .uri("/v1/messages")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);
            return json.path("content").get(0).path("text").asText();

        } catch (Exception e) {
            log.error("Erreur appel Claude: {}", e.getMessage());
            throw new RuntimeException("Claude API error: " + e.getMessage(), e);
        }
    }

    public <T> T completeAsJson(String systemPrompt, String userMessage, Class<T> type) {
        String raw = complete(systemPrompt, userMessage);
        try {
            // Extraire le JSON si enveloppé dans du markdown
            String json = extractJson(raw);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("Erreur parsing JSON Claude: {}\nRéponse brute: {}", e.getMessage(), raw);
            throw new RuntimeException("Failed to parse Claude JSON response", e);
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        // Essayer avec tableau JSON
        start = text.indexOf('[');
        end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
