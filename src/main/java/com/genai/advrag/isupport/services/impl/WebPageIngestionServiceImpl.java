package com.genai.advrag.isupport.services.impl;

import com.genai.advrag.isupport.Constant;
import com.genai.advrag.isupport.config.IngestionProperties;
import com.genai.advrag.isupport.config.IngestionResult;
import com.genai.advrag.isupport.exception.DocumentNotFoundException;
import com.genai.advrag.isupport.exception.EmptyWebPageException;
import com.genai.advrag.isupport.model.IngestedDocument;
import com.genai.advrag.isupport.repository.IngestedDocumentRepository;
import com.genai.advrag.isupport.services.WebPageIngestionService;
import com.genai.advrag.isupport.utility.WebPageDocumentReader;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class WebPageIngestionServiceImpl implements WebPageIngestionService {

    private static final Logger log= LoggerFactory.getLogger(WebPageIngestionServiceImpl.class);

    private IngestedDocumentRepository ledger;

    private VectorStore vectorStore;

    private JdbcTemplate jdbcTemplate;

    private IngestionProperties properties;

    @Autowired
    private WebPageDocumentReader webPageDocumentReader;


    public WebPageIngestionServiceImpl(IngestedDocumentRepository ledger,
                                   VectorStore vectorStore,
                                   JdbcTemplate jdbcTemplate,
                                   IngestionProperties properties)
    {
        this.ledger=ledger;
        this.vectorStore=vectorStore;
        this.jdbcTemplate=jdbcTemplate;
        this.properties=properties;
    }

    public IngestionResult ingest(String url, String userId) {

        List<Document> pages = readPages(url);

        if (pages.isEmpty()) {
            throw new EmptyWebPageException(url);
        }

        FileFingerprint fingerprint = fingerprint(pages);

        log.info("Fingerprint for url {}, userId {} is {}",
                url,
                userId,
                fingerprint.shortHash());

        Optional<IngestedDocument> alreadyIngested =
                ledger.findByUserIdAndDocumentHash(
                        userId,
                        fingerprint.hash);

        if (alreadyIngested.isPresent()) {

            log.info("Skipping ingestion for {}", url);

            return IngestionResult
                    .skippedDuplicateIngest(alreadyIngested.get());
        }

        List<Document> chunks = split(
                pages,
                fingerprint.hash(),
                url,
                userId);

        if (chunks.isEmpty()) {
            throw new EmptyWebPageException(url);
        }

        vectorStore.add(chunks);

        return record(
                fingerprint,
                url,
                userId,
                pages.size(),
                chunks);
    }

    private String calculateHash(String text) {
        try {
            String normalized = text.replaceAll("\\s+", " ").trim();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(normalized.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional
    @Override
    public void deleteDocument(Long documentId,
                               String userId) {


        IngestedDocument document =
                requiredDocument(documentId, userId);

        // Delete all embeddings
        // from the Vector Database.
        int deletedChunks =
                jdbcTemplate.update(

                        "DELETE FROM vector_store "
                                + "WHERE metadata ->> 'documentHash' = ? "
                                + "AND metadata ->> 'userId' = ?",

                        document.getDocumentHash(),
                        userId);

        // Delete document metadata
        // from relational database.
        ledger.delete(document);

        // Print log message.
        log.info("Deleted document {} ('{}') and {} chunks",
                documentId,
                document.getSourceIdentifier(),
                deletedChunks);
    }

    private List<Document> readPages(String url) {
        return webPageDocumentReader.read(url);
    }

    private FileFingerprint fingerprint(List<Document> pages) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            long size = 0;

            for (Document page : pages) {

                String text = page.getText();

                if (!StringUtils.hasText(text)) {
                    continue;
                }

                String normalized = text
                        .replaceAll("\\s+", " ")
                        .trim();

                byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);

                digest.update(bytes);

                size += bytes.length;
            }

            String hash = HexFormat.of().formatHex(digest.digest());

            return new FileFingerprint(hash, size);

        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record FileFingerprint(String hash, long sizeBytes)
    {
        String shortHash()
        {
            return hash.substring(0,12);
        }
    }

    private List<Document> split(List<Document> pages, String documentHash, String srcIdentifier, String userId)
    {
        TokenTextSplitter splitter = TokenTextSplitter
                .builder()
                .withChunkSize(properties.chunkSize())
                .build();
        return splitter.apply(pages).stream()
                .filter(chunk -> StringUtils.hasText(chunk.getText()))
                .map(chunk ->
                {
                    Map<String, Object> metadata= new HashMap<>(chunk.getMetadata());
                    metadata.put(Constant.META_USER_ID, userId);
                    metadata.put(Constant.META_DOCUMENT_HASH, documentHash);
                    metadata.put(Constant.META_SRC_PROVIDER, srcIdentifier);
                    metadata.put(Constant.META_SRC_SOURCE_TYPE, IngestedDocument.SourceType.WEB);

                    return new Document(chunk.getText(), metadata);
                })
                .toList();

    }

    private IngestionResult record(FileFingerprint fingerprint, String srcIdentifier, String userId, int pageCount, List<Document> chunks)
    {
        IngestedDocument document = new IngestedDocument(fingerprint.hash,
                IngestedDocument.SourceType.WEB,
                srcIdentifier,
                srcIdentifier,
                userId,
                fingerprint.sizeBytes(),
                pageCount,
                chunks.size());
        try
        {
            IngestedDocument saved = ledger.saveAndFlush(document);
            log.info("Ingested as document ", srcIdentifier, saved.getId(), saved.getChunkCount());
            return IngestionResult.ingest(saved);
        }
        catch(DataIntegrityViolationException raceLost)
        {
            log.warn("Concurrent ingestion won the race", srcIdentifier
            );
            deleteChunks(chunks);
            return ledger.findByUserIdAndDocumentHash(userId, fingerprint.hash())
                    .map(IngestionResult::skippedDuplicateIngest)
                    .orElseThrow(()->raceLost );

        }
    }
    private void deleteChunks(List<Document> chunks) {

        vectorStore.delete(
                chunks.stream()
                        .map(Document::getId)
                        .toList());
    }

    public IngestedDocument requiredDocument(Long documentId, String userId)
    {
        return ledger.findById(documentId)
                .filter(doc -> doc.getId().equals(userId))

                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    @Override
    public List<IngestedDocument> listDocument(String userId)
    {
        return ledger.findByUserIdOrderByIngestedAtDesc(userId);
    }
}
