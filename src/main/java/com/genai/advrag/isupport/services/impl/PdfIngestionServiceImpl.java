package com.genai.advrag.isupport.services.impl;

import com.genai.advrag.isupport.Constant;
import com.genai.advrag.isupport.config.IngestionProperties;
import com.genai.advrag.isupport.config.IngestionResult;
import com.genai.advrag.isupport.exception.DocumentNotFoundException;
import com.genai.advrag.isupport.exception.EmptyPdfException;
import com.genai.advrag.isupport.model.IngestedDocument;
import com.genai.advrag.isupport.repository.IngestedDocumentRepository;
import com.genai.advrag.isupport.services.PdfIngestionService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

@Service
public class PdfIngestionServiceImpl implements PdfIngestionService {

    private static final Logger log= LoggerFactory.getLogger(PdfIngestionServiceImpl.class);

    private IngestedDocumentRepository ledger;

    private VectorStore vectorStore;

    private JdbcTemplate jdbcTemplate;

    private IngestionProperties properties;


    public PdfIngestionServiceImpl(IngestedDocumentRepository ledger,
                               VectorStore vectorStore,
                               JdbcTemplate jdbcTemplate,
                               IngestionProperties properties )
    {
        this.ledger=ledger;
        this.vectorStore=vectorStore;
        this.jdbcTemplate=jdbcTemplate;
        this.properties=properties;
    }

    @Override
    public IngestionResult ingest(Resource pdf, String filename, String userId) {
    FileFingerprint fingerprint= fingerprint(pdf);
    log.info("Fingerprint for file {}, userID {} is {}", filename,userId, fingerprint.shortHash());
    Optional<IngestedDocument> alreadyIngested= ledger.findByUserIdAndDocumentHash(userId, fingerprint.hash);
    log.info("Already ingested document for file {}, userID {} is {}", filename,userId, alreadyIngested.toString());
    if(alreadyIngested.isPresent())
    {
        log.info("Skipping as document is ingested", filename, alreadyIngested.get().getId(),
                alreadyIngested.get().getIngestedAt());
        System.out.println("Skipped ingestion");
        return IngestionResult.skippedDuplicateIngest(alreadyIngested.get());
    }

        List<Document> pages = readPages(pdf);
        if(pages.isEmpty())
        {
            throw new EmptyPdfException(filename);
        }
        List<Document> chunks= split(pages, fingerprint.hash(), filename, userId);
        if(chunks.isEmpty())
        {
            throw new EmptyPdfException(filename);
        }
        vectorStore.add(chunks);
        System.out.println("Ingested");
        return record(fingerprint, filename, userId, pages.size(), chunks);
    }
    private IngestionResult record(FileFingerprint fingerprint, String filename, String userId, int pageCount, List<Document> chunks)
    {
        IngestedDocument document = new IngestedDocument(fingerprint.hash,
                IngestedDocument.SourceType.PDF,
                filename,
                filename,
                userId,
                fingerprint.sizeBytes(),
                pageCount,
                chunks.size());
        try
        {
            IngestedDocument saved = ledger.saveAndFlush(document);
            log.info("Ingested as document ", filename, saved.getId(), saved.getChunkCount());
            return IngestionResult.ingest(saved);
        }
        catch(DataIntegrityViolationException raceLost)
        {
            log.warn("Concurrent ingestion won the race", filename
            );
            deleteChunks(chunks);
            return ledger.findByUserIdAndDocumentHash(userId, fingerprint.hash())
                    .map(IngestionResult::skippedDuplicateIngest)
                    .orElseThrow(()->raceLost );

        }
    }


    private List<Document> split(List<Document> pages, String fileHash, String filename, String userId)
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
                    metadata.put(Constant.META_DOCUMENT_HASH, fileHash);
                    metadata.put(Constant.META_SRC_PROVIDER, filename);
                    metadata.put(Constant.META_SRC_SOURCE_TYPE, IngestedDocument.SourceType.PDF);

                    return new Document(chunk.getText(), metadata);
                })
                .toList();

    }

    private FileFingerprint fingerprint(Resource pdf)
    {
        try(InputStream inputStream = pdf.getInputStream())
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // read the pdf 8kb at a time or in any small blocks
            byte[] buffer = new byte[8192];

            long size=0;

            int read;

            // read till end of the file
            while((read = inputStream.read(buffer)) !=-1)
            {
                digest.update(buffer, 0, read);

                size +=read;
            }
            return new FileFingerprint(HexFormat.of().formatHex(digest.digest()), size);


        }
        catch(Exception e)
        {
            throw new RuntimeException("Some issue ", e);

        }
    }
    private List<Document> readPages(Resource pdf)
    {
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(properties.pagesPerDocument())
                .build();

        return new PagePdfDocumentReader(pdf, config).get();
    }

    @Override
    public List<IngestedDocument> listDocument(String userId)
    {
        return ledger.findByUserIdOrderByIngestedAtDesc(userId);
    }

    public IngestedDocument requiredDocument(Long documentId, String userId)
    {
        return ledger.findById(documentId)
                .filter(doc -> doc.getId().equals(userId))

                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }
    private record FileFingerprint(String hash, long sizeBytes)
    {
        String shortHash()
        {
            return hash.substring(0,12);
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
    private void deleteChunks(List<Document> chunks) {

        vectorStore.delete(
            chunks.stream()
                .map(Document::getId)
                .toList());
    }

}
