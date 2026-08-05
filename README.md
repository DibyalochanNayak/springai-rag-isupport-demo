# Building a Source-Agnostic Knowledge Ingestion Framework with Spring AI & Advanced RAG for an AI-Powered L3 Support Assistant (iSupport)

## Problem statement

Enterprises store knowledge in many formats and silos (PDFs, web pages, Word, Confluence, SharePoint, Markdown). Building a reusable ingestion pipeline that normalizes diverse sources, deduplicates content, chunks and embeds text, and exposes a simple RAG interface is hard. iSupport demonstrates a compact, source-agnostic ingestion framework that fingerprints content to avoid duplicate embeddings, stores embeddings with metadata in a vector store, and exposes APIs for ingestion, management, and retrieval-augmented generation (RAG) chat.

## Real-world use case

As part of developing iSupport—an AI-powered L3 Support Assistant—I designed a reusable ingestion framework that consolidates knowledge from PDFs, Confluence, SharePoint, Help Center articles, and web pages into a centralized knowledge base.

Using an Advanced RAG pipeline, iSupport retrieves the most relevant context from ingested enterprise documents, enabling engineers to receive faster, more accurate, and context-aware answers instead of relying solely on an LLM's pretrained knowledge.. Fingerprinting avoids repeated ingestion and reduces embedding costs.

## Architecture

```text
┌────────────────────────────────────────────────────────────┐
│ PDF │ Confluence │ SharePoint │ Help Center │ Any Web      │
└───────────────────────┬────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ Document Readers | Web Page Reader                         │
│ (Strategy Pattern)                                         │
└───────────────────────┬────────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────────┐
│ Generic Ingestion Pipeline                                 │
│ Spring AI Documents → SHA-256 → Chunk → Embed → Persist    │
│ Template Method  • Content-based Deduplication        │
└───────────────────────┬────────────────────────────────────┘
                        │
                        ▼
         Vector Store + Unified Metadata Repository
```




- Ingestion endpoints (PDF upload, webpage ingest) implemented as REST controllers.
- Source readers normalize and produce pages/documents (PDF reader, web reader, etc.).
- Fingerprinting computes a SHA-256 hash over normalized text to detect duplicates.
- Chunking splits pages using a token-based splitter and attaches metadata (userId, documentHash, source).
- Vector store (pgvector) stores embeddings and metadata; a relational ledger (JPA entity IngestedDocument) records document metadata and ingestion state.
- RAG/chat component queries the vector store with filters scoped by userId and optional documentId and uses a chat model advisor to produce answers constrained to retrieved context.

## Techstack

- Java 17+ with Spring Boot
- Spring AI (chat, embeddings, vectorstore)
- Spring Data JPA (Jakarta Persistence)
- PostgreSQL + pgvector
- TokenTextSplitter for chunking
- Maven (mvnw wrapper)
- Docker for running pgvector/Postgres
- OpenAI models (via spring.ai.openai)

## Design Principles

✔ Strategy Pattern – Each source implements its own DocumentReader, making new integrations plug-and-play.

✔ Template Method Pattern – A common ingestion workflow (Read → Fingerprint → Chunk → Embed → Persist) is reused across all content sources.

✔ Single Responsibility Principle (SRP) – Reading, orchestration, chunking, persistence, and vector storage are cleanly separated.

## What this architecture achieves

✅ Source-agnostic ingestion

✅ Content-based duplicate detection using SHA-256

✅ One reusable ingestion pipeline for every knowledge source

✅ Unified metadata repository for tracking ingested content

✅ Easily extensible to new sources with minimal code changes

## Build & run guide

1. Start pgvector Postgres (from project root):
   docker-compose up -d

2. Set OpenAI API key (PowerShell):
   $env:OPENAI_API_KEY = "sk-..."

3. Build:
   .\mvnw.cmd -DskipTests package

4. Run:
   java -jar .\target\isupport-0.0.1-SNAPSHOT.jar
   (or) .\mvnw.cmd spring-boot:run

5. Key endpoints:
   - POST /api/webpages/ingest (JSON {"url":"...","userId":"..."})
   - POST /api/docs/upload (multipart file + userId)
   - GET /api/docs/list?userId=...
   - GET /api/webpages/list?userId=...
   - DELETE /api/docs/{id}?userId=...
   - GET /api/rag/chat?query=...&userId=...[&documentId=...]

