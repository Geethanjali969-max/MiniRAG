
package com.example.minirag;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PdfController {

    private final TextChunker textChunker;
    private final DocumentStore documentStore;

    public PdfController(
            TextChunker textChunker,
            DocumentStore documentStore) {
        this.textChunker = textChunker;
        this.documentStore = documentStore;
    }

    @PostMapping(
        value = "/api/pdf/extract",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public List<String> extractPdf(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Please upload a PDF file."
            );
        }

        if (file.getOriginalFilename() == null ||
            !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only PDF files are allowed."
            );
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            if (extractedText.isBlank()) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No readable text found in this PDF."
                );
            }

            List<String> chunks = textChunker.splitText(extractedText);

            documentStore.saveChunks(chunks);

            return chunks;

        } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Could not read this PDF file."
            );
        }
    }
}
