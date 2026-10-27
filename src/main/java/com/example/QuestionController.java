
package com.example.minirag;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class QuestionController {

    private final Retriever retriever;
    private final DocumentStore documentStore;
    private final GeminiService geminiService;

    public QuestionController(
            Retriever retriever,
            DocumentStore documentStore,
            GeminiService geminiService) {
        this.retriever = retriever;
        this.documentStore = documentStore;
        this.geminiService = geminiService;
    }

    public record QuestionRequest(String question) {}

    @PostMapping("/api/question/search")
    public String search(@RequestBody QuestionRequest request) {

        if (request == null || request.question() == null
                || request.question().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please provide a question.");
        }

        if (!documentStore.hasDocument()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please upload a PDF before asking a question.");
        }

        List<String> relevantChunks = retriever.retrieve(
                request.question(),
                documentStore.getChunks(),
                3);

        if (relevantChunks.isEmpty()) {
            return "The uploaded document does not contain enough "
                    + "information to answer this question.";
        }

        try {
            return geminiService.generateAnswer(
                    request.question(), relevantChunks);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not communicate with the AI service. "
                            + "Please check your connection.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI request was interrupted.");

        } catch (IllegalStateException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    e.getMessage());
        }
    }
}
