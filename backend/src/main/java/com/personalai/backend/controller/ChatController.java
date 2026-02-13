package com.personalai.backend.controller;

import com.personalai.backend.dto.ChatRequest;
import com.personalai.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST controller for chat interactions with AI assistant.
 * 
 * This controller:
 * - Maintains backward compatibility with existing API
 * - Uses ChatService interface for AI interactions
 * - Handles reactive responses with proper error handling
 * - Returns structured responses matching the original format
 * 
 * Requirements: 7.1, 8.3, 9.1
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * Process a chat request and return AI response.
     * 
     * This endpoint:
     * - Accepts POST requests with ChatRequest body
     * - Returns response in format: {"message": {"role": "assistant", "content": "..."}}
     * - Handles errors reactively with structured error responses
     * 
     * @param chatRequest the incoming chat request with message history
     * @return Mono containing ResponseEntity with chat response or error
     */
    @PostMapping
    public Mono<ResponseEntity<Object>> chat(@RequestBody ChatRequest chatRequest) {
        log.debug("Received chat request with {} messages", 
                chatRequest.getMessages() != null ? chatRequest.getMessages().size() : 0);
        
        return chatService.getChatCompletion(chatRequest)
                .map(content -> {
                    log.debug("Chat request successful, response length: {}", content.length());
                    return ResponseEntity.ok(
                            (Object) Map.of("message", 
                                    Map.of("role", "assistant", "content", content)));
                })
                .onErrorResume(e -> {
                    log.error("Chat request failed: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(Map.of("error", e.getMessage())));
                });
    }

    /**
     * Process a chat request with streaming response.
     * 
     * This endpoint:
     * - Accepts POST requests with ChatRequest body
     * - Returns response chunks progressively as they are generated
     * - Uses Server-Sent Events (SSE) for streaming
     * - Useful for real-time user interfaces
     * 
     * @param chatRequest the incoming chat request with message history
     * @return Flux emitting response chunks as they are generated
     */
    @PostMapping("/stream")
    public reactor.core.publisher.Flux<String> chatStream(@RequestBody ChatRequest chatRequest) {
        log.debug("Received streaming chat request with {} messages", 
                chatRequest.getMessages() != null ? chatRequest.getMessages().size() : 0);
        
        return chatService.getChatCompletionStream(chatRequest)
                .doOnComplete(() -> log.debug("Streaming chat request completed"))
                .doOnError(e -> log.error("Streaming chat request failed: {}", e.getMessage(), e));
    }
}
