package com.genai.advrag.isupport.services;

import com.genai.advrag.isupport.config.IngestionResult;
import com.genai.advrag.isupport.model.IngestedDocument;
import org.springframework.core.io.Resource;

import java.util.List;

public interface WebPageIngestionService {
    IngestionResult ingest(String url, String userId);
    void deleteDocument(Long documentId,
                        String userId);

    List<IngestedDocument> listDocument(String userId);
}
