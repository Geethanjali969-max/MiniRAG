
package com.example.minirag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class Retriever {

    public List<String> retrieve(
            String question,
            List<String> chunks,
            int topK) {

        if (question == null || question.isBlank()
                || chunks == null || chunks.isEmpty()
                || topK <= 0) {
            return List.of();
        }

        String[] words = question.toLowerCase(Locale.ROOT)
                .split("[^a-z0-9]+");

        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (String chunk : chunks) {
            String lowerChunk = chunk.toLowerCase(Locale.ROOT);
            int score = 0;

            for (String word : words) {
                if (word.length() < 2) {
                    continue;
                }

                if (lowerChunk.contains(word)) {
                    score++;
                }
            }

            if (score > 0) {
                scoredChunks.add(new ScoredChunk(chunk, score));
            }
        }

        scoredChunks.sort(
                Comparator.comparingInt(ScoredChunk::score).reversed()
        );

        List<String> results = new ArrayList<>();

        for (int i = 0; i < Math.min(topK, scoredChunks.size()); i++) {
            results.add(scoredChunks.get(i).text());
        }

        return results;
    }

    private record ScoredChunk(String text, int score) {
    }
}
