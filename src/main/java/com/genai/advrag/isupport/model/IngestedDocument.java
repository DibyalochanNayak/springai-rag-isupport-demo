package com.genai.advrag.isupport.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingested_documents")
public class IngestedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_hash", nullable = false, length = 64)
    private String documentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_identifier", nullable = false, length = 2048)
    private String sourceIdentifier;

    @Column(length = 500)
    private String title;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "content_size_bytes", nullable = false)
    private long contentSizeBytes;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "ingested_at", nullable = false)
    private LocalDateTime ingestedAt = LocalDateTime.now();

    /** Required by JPA. */
    protected IngestedDocument() {
    }

    public IngestedDocument(String documentHash, SourceType sourceType, String sourceIdentifier, String title,
                            String userId, long contentSizeBytes, int pageCount, int chunkCount) {
        this.documentHash = documentHash;
        this.sourceType = sourceType;
        this.sourceIdentifier = sourceIdentifier;
        this.title = title;
        this.userId = userId;
        this.contentSizeBytes = contentSizeBytes;
        this.pageCount = pageCount;
        this.chunkCount = chunkCount;
    }

    public enum SourceType {
        PDF,
        WEB,
        WORD,
        MARKDOWN,
        CONFLUENCE,
        SHAREPOINT
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getContentSizeBytes() {
        return contentSizeBytes;
    }

    public void setContentSizeBytes(long contentSizeBytes) {
        this.contentSizeBytes = contentSizeBytes;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public LocalDateTime getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(LocalDateTime ingestedAt) {
        this.ingestedAt = ingestedAt;
    }

    @Override
    public String toString() {
        return "IngestedDocument{" +
                "id=" + id +
                ", documentHash='" + documentHash + '\'' +
                ", sourceType=" + sourceType +
                ", sourceIdentifier='" + sourceIdentifier + '\'' +
                ", title='" + title + '\'' +
                ", userId='" + userId + '\'' +
                ", contentSizeBytes=" + contentSizeBytes +
                ", pageCount=" + pageCount +
                ", chunkCount=" + chunkCount +
                ", ingestedAt=" + ingestedAt +
                '}';
    }
}
