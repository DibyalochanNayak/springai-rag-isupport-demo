package com.genai.advrag.isupport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ingestion")
public record IngestionProperties(String inboxFolder, String defaultUserId, int chunkSize, int pagesPerDocument) {
}
