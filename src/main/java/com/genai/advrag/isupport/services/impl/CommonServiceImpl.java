package com.genai.advrag.isupport.services.impl;

import com.genai.advrag.isupport.exception.DocumentNotFoundException;
import com.genai.advrag.isupport.model.IngestedDocument;
import com.genai.advrag.isupport.repository.IngestedDocumentRepository;
import com.genai.advrag.isupport.services.CommonService;
import org.springframework.stereotype.Service;

@Service
public class CommonServiceImpl implements CommonService {

    private IngestedDocumentRepository ledger;
    @Override
    public IngestedDocument requiredDocument(Long documentId, String userId) {
        return ledger.findById(documentId)
                .filter(doc -> doc.getId().equals(userId))

                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }
}
