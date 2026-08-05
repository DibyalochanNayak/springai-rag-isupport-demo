package com.genai.advrag.isupport.controller;

import com.genai.advrag.isupport.config.IngestionResult;
import com.genai.advrag.isupport.model.IngestedDocument;
import com.genai.advrag.isupport.services.WebPageIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/webpages")
public class WebPageIngestionController {
    @Autowired
    private  WebPageIngestionService service;

    @PostMapping("/ingest")
    public ResponseEntity<DocumentResponse> ingest(
            @RequestBody WebPageRequest request) {

        validate(request);

        IngestionResult result =
                service.ingest(request.url(), request.userId());

        return result.wasSkipped()
                ? ResponseEntity.ok(DocumentResponse.of(result))
                : ResponseEntity.status(HttpStatus.CREATED)
                .body(DocumentResponse.of(result));
    }

    @GetMapping("/list")
    public List<DocumentResponse> list(
            @RequestParam String userId) {

        return service.listDocument(userId)
                .stream()
                .map(DocumentResponse::of)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam String userId) {

        service.deleteDocument(id, userId);

        return ResponseEntity.noContent().build();
    }

    private void validate(WebPageRequest request) {

        if (!StringUtils.hasText(request.url())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "URL is required");
        }

        try {
            URI uri = URI.create(request.url());

            if (uri.getScheme() == null ||
                    (!uri.getScheme().equalsIgnoreCase("http")
                            && !uri.getScheme().equalsIgnoreCase("https"))) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only HTTP/HTTPS URLs are supported");
            }

        } catch (IllegalArgumentException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid URL");
        }
    }

    public record WebPageRequest(
            String url,
            String userId) {
    }
    public record DocumentResponse(Long id, String filename, int pageCount, int chunkCount, long fileSizeBytes, String status)
    {
        static DocumentResponse of(IngestionResult result){
            return of(result.document(), result.status().name());
        }
        static DocumentResponse of(IngestedDocument document){
            return of(document, "INGESTED");
        }
        private static DocumentResponse of(IngestedDocument d, String status)
        {
            return new DocumentResponse(d.getId(), d.getSourceIdentifier(), d.getPageCount(), d.getChunkCount(), d.getContentSizeBytes(), status);
        }
    }
}
