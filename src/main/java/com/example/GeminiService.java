
package com.example.minirag;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String generateAnswer(
            String question, List<String> context)
            throws IOException, InterruptedException {

        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is missing from the application terminal.");
        }

        if (context == null || context.isEmpty()) {
            return "The uploaded document does not contain enough "
                    + "information to answer this question.";
        }

        String prompt = """
                You are MiniRAG, an assistant that answers questions
                using only the supplied document context.

                Explain answers in simple language. Do not invent facts.
                If the answer is absent, say that the document does not
                contain enough information.

                DOCUMENT CONTEXT:
                %s

                QUESTION:
                %s
                """.formatted(String.join("\n\n", context), question);

        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        content.putArray("parts")
                .addObject()
                .put("text", prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            // Report the API response without revealing the API key.
            String details = response.body();

            if (details == null || details.isBlank()) {
                details = "No error details were returned.";
            }

            throw new IllegalStateException(
                    "Gemini returned HTTP " + response.statusCode()
                            + ". Details: " + details);
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode answer = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (!answer.isTextual() || answer.asText().isBlank()) {
            throw new IllegalStateException(
                    "Gemini returned no answer text. Response: "
                            + response.body());
        }

        return answer.asText();
    }
}
