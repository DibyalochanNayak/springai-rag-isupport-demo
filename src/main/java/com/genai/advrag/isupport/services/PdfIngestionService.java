package com.genai.advrag.isupport.services;

import com.genai.advrag.isupport.config.IngestionResult;
import com.genai.advrag.isupport.model.IngestedDocument;
import com.genai.advrag.isupport.services.impl.PdfIngestionServiceImpl;
import org.springframework.core.io.Resource;

import java.util.List;

public interface PdfIngestionService {
    IngestionResult ingest(Resource pdf, String filename, String userId);

    List<IngestedDocument> listDocument(String userId);

    void deleteDocument(Long documentId,
                        String userId);
}
