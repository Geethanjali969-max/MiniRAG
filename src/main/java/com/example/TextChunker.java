
package com.example.minirag;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;

    public List<String> splitText(String text) {

        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String cleanedText = text.replaceAll("\\s+", " ").trim();

        int start = 0;

        while (start < cleanedText.length()) {

            int end = Math.min(
                start + CHUNK_SIZE,
                cleanedText.length()
            );

            String chunk = cleanedText.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end == cleanedText.length()) {
                break;
            }

            start = end - OVERLAP;
        }

        return chunks;
    }
}
