package com.budgetbrain.budgetbrain_api.service;

import com.budgetbrain.budgetbrain_api.model.Transaction;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InsightsService {

    private final WebClient geminiWebClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    public InsightsService(@Qualifier("geminiWebClient") WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
    }

    public String generateInsight(List<Transaction> transactions) {
        String summary = buildSpendingSummary(transactions);
        String prompt = "Here is a summary of a user's recent transactions by category:\n\n"
                + summary
                + "\n\nWrite a short, friendly 2-3 sentence insight about their spending. "
                + "Mention the highest-spending category and one practical observation. "
                + "Do not use markdown formatting.";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "maxOutputTokens", 300,
                        "temperature", 0.7,
                        "thinkingConfig", Map.of("thinkingBudget", 0)
                )
        );

        Map<String, Object> response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/gemini-flash-latest:generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
//        System.out.println("RAW GEMINI RESPONSE: " + response);

        return extractTextFromResponse(response);
    }

    private String buildSpendingSummary(List<Transaction> transactions) {
        Map<String, Double> totalsByCategory = transactions.stream()
                .filter(t -> t.getAmount() < 0)  // only spending, not income
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(t -> Math.abs(t.getAmount()))
                ));

        StringBuilder sb = new StringBuilder();
        totalsByCategory.forEach((category, total) ->
                sb.append(String.format("%s: %.2f%n", category, total))
        );
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.getFirst().get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.getFirst().get("text");
        } catch (Exception e) {
            return "Could not generate insight: " + e.getMessage();
        }
    }
}