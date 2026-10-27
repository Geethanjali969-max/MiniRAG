
package com.example.minirag;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DocumentStore {

    private List<String> chunks = List.of();

    public synchronized void saveChunks(List<String> newChunks) {
        chunks = List.copyOf(newChunks);
    }

    public synchronized List<String> getChunks() {
        return chunks;
    }

    public synchronized boolean hasDocument() {
        return !chunks.isEmpty();
    }
}
