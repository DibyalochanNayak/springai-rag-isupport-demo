package com.genai.advrag.isupport.repository;

import com.genai.advrag.isupport.model.IngestedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngestedDocumentRepository extends JpaRepository<IngestedDocument, Long> {

   Optional<IngestedDocument> findByUserIdAndDocumentHash(String userId, String documentHash);
   List<IngestedDocument> findByUserIdOrderByIngestedAtDesc(String userId);


}
