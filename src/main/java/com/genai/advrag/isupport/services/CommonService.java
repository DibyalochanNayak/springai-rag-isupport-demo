package com.genai.advrag.isupport.services;

import com.genai.advrag.isupport.model.IngestedDocument;

public interface CommonService {

    IngestedDocument requiredDocument(Long documentId, String userId);
}
