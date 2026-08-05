package com.genai.advrag.isupport.controller;

import com.genai.advrag.isupport.Constant;
import com.genai.advrag.isupport.services.CommonService;
import com.genai.advrag.isupport.services.PdfIngestionService;
import com.genai.advrag.isupport.services.WebPageIngestionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class ChatController {


    private ChatClient chatClient;
    private VectorStore vectorStore;
    @Autowired
    private PdfIngestionService pdfIngestionService;
    @Autowired
    private WebPageIngestionService webPageIngestionService;
    @Autowired
    private CommonService commonService;

    public ChatController(OpenAiChatModel chatModel, VectorStore vectorStore)
    {
        this.chatClient= ChatClient.create(chatModel);
        this.vectorStore=vectorStore;

    }

    @GetMapping("/chat")
    public String ask(@RequestParam String query
            , @RequestParam String userId,
                      @RequestParam(required = false) Long documentId)
    {
        return chatClient.prompt(query)
                .system(
                        """
            You are answering questions about documents the user has uploaded OR the user has ingested URL.
            Answer using ONLY the retrieved document context.
            If the answer is not in the context Response in short and polite manner, saying can't help as context is not available. Either ingest data by uploading document or ingesting URL to get the answer.            
            In each your response, mentioned the source identifier.
            """
                ).advisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .topK(8)
                                        .similarityThreshold(0.25)
                                        .filterExpression(scopeFilter(userId, documentId))
                                        .build())
                                .build()).call().content();
    }
    private Filter.Expression scopeFilter(String userId,
                                          Long documentId) {

        // Create a Filter Builder.
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        if (documentId == null) {

            return b.eq(
                            Constant.META_USER_ID,
                            userId)
                    .build();
        }
        String documentHash =
                commonService
                        .requiredDocument(documentId, userId)
                        .getDocumentHash();
        return b.and(

                b.eq(
                        Constant.META_USER_ID,
                        userId),

                b.eq(
                        Constant.META_DOCUMENT_HASH,
                        documentHash)

        ).build();
    }
}
