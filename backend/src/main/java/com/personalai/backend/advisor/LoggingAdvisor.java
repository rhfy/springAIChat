package com.personalai.backend.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Advisor that logs request and response details for chat interactions.
 * 
 * This advisor captures:
 * - Request details: user message content, timestamp
 * - Response details: assistant message content, model used, duration
 * 
 * Logging is performed at INFO level for successful requests and ERROR level for failures.
 * 
 * Requirements: 3.5, 7.4, 7.5
 */
public class LoggingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(LoggingAdvisor.class);
    private static final String ADVISOR_NAME = "LoggingAdvisor";

    @Override
    public String getName() {
        return ADVISOR_NAME;
    }

    @Override
    public int getOrder() {
        // Execute early in the chain to capture all requests
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        Instant startTime = Instant.now();
        
        // Log request details
        logRequest(chatClientRequest, startTime);
        
        try {
            // Proceed with the advisor chain
            ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
            
            // Calculate duration
            Duration duration = Duration.between(startTime, Instant.now());
            
            // Log response details
            logResponse(response, duration);
            
            return response;
        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("Chat request failed after {}ms: {}", duration.toMillis(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Instant startTime = Instant.now();
        
        // Log request details
        logRequest(chatClientRequest, startTime);
        
        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnComplete(() -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.info("Chat stream completed in {}ms", duration.toMillis());
                })
                .doOnError(e -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.error("Chat stream failed after {}ms: {}", duration.toMillis(), e.getMessage(), e);
                });
    }

    /**
     * Logs request details including user message and timestamp.
     * 
     * @param chatClientRequest the request being processed
     * @param timestamp the request timestamp
     */
    private void logRequest(ChatClientRequest chatClientRequest, Instant timestamp) {
        log.info("Chat request received at {}", timestamp);
        
        if (chatClientRequest.prompt() != null && !chatClientRequest.prompt().getInstructions().isEmpty()) {
            String userMessage = chatClientRequest.prompt().getInstructions().stream()
                    .filter(msg -> "user".equals(msg.getMessageType().getValue()))
                    .map(msg -> msg.getText())
                    .findFirst()
                    .orElse("No user message");
            log.debug("User message: {}", userMessage);
        }
        
        Map<String, Object> adviseContext = chatClientRequest.context();
        if (adviseContext != null && !adviseContext.isEmpty()) {
            log.debug("Advise context: {}", adviseContext);
        }
    }

    /**
     * Logs response details including assistant message, model used, and duration.
     * 
     * @param chatClientResponse the response from the model
     * @param duration the time taken to process the request
     */
    private void logResponse(ChatClientResponse chatClientResponse, Duration duration) {
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        
        if (chatResponse != null && chatResponse.getResult() != null) {
            String assistantMessage = chatResponse.getResult().getOutput().getText();
            String model = extractModelName(chatResponse);
            
            log.info("Chat response generated in {}ms using model: {}", 
                    duration.toMillis(), model);
            log.debug("Assistant message: {}", assistantMessage);
            
            // Log token usage if available
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                log.debug("Token usage - Prompt: {}, Completion: {}, Total: {}",
                        chatResponse.getMetadata().getUsage().getPromptTokens(),
                        chatResponse.getMetadata().getUsage().getCompletionTokens(),
                        chatResponse.getMetadata().getUsage().getTotalTokens());
            }
        } else {
            log.warn("Chat response completed in {}ms but contained no result", duration.toMillis());
        }
    }

    /**
     * Extracts the model name from the chat response.
     * 
     * @param chatResponse the response from the model
     * @return the model name or "unknown" if not available
     */
    private String extractModelName(ChatResponse chatResponse) {
        if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getModel() != null) {
            return chatResponse.getMetadata().getModel();
        }
        return "unknown";
    }
}
