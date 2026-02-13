package com.personalai.backend.service;

import com.personalai.backend.dto.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of ChatService using Spring AI's ChatClient fluent API.
 * 
 * This service:
 * - Uses ChatClient's fluent API for all AI interactions
 * - Integrates with configured advisors (logging, retry, memory, observability)
 * - Supports conversation memory management
 * - Handles MCP tool integration when available
 * - Provides reactive responses using Project Reactor
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.5, 2.1, 8.1, 8.2
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    
    private final ChatClient chatClient;

    /**
     * Creates a ChatServiceImpl with configured ChatClient.
     * 
     * The ChatClient is built with:
     * - Default system prompt from SystemPromptProvider
     * - All configured advisors (logging, retry, memory, observability)
     * - MCP tool callbacks if available
     * 
     * @param chatClientBuilder the ChatClient builder
     * @param systemPromptProvider provider for system prompt
     * @param advisors list of configured advisors
     * @param toolCallbackProvider optional MCP tool callback provider
     */
    public ChatServiceImpl(
            ChatClient.Builder chatClientBuilder,
            SystemPromptProvider systemPromptProvider,
            List<Advisor> advisors,
            Optional<ToolCallbackProvider> toolCallbackProvider) {
        
        log.info("Initializing ChatServiceImpl with {} advisors", advisors.size());
        
        // Build ChatClient with fluent configuration
        ChatClient.Builder builder = chatClientBuilder
                .defaultSystem(systemPromptProvider.getSystemPrompt())
                .defaultAdvisors(advisors.toArray(new Advisor[0]));
        
        // Add MCP tools if available
        toolCallbackProvider.ifPresentOrElse(
                provider -> {
                    builder.defaultToolCallbacks(provider);
                    log.info("MCP tools registered: {} tools available", 
                            provider.getToolCallbacks().length);
                },
                () -> log.info("No MCP tools available")
        );
        
        this.chatClient = builder.build();
        log.info("ChatClient initialized successfully");
    }

    @Override
    public Mono<String> getChatCompletion(ChatRequest chatRequest) {
        if (chatRequest == null || chatRequest.getMessages() == null || chatRequest.getMessages().isEmpty()) {
            return Mono.error(new IllegalArgumentException("ChatRequest must contain at least one message"));
        }
        
        return Mono.fromCallable(() -> {
            log.debug("Processing chat request with {} messages", chatRequest.getMessages().size());
            
            // Start building the prompt using fluent API
            var promptSpec = chatClient.prompt();
            
            // Add message history using fluent API
            // Process messages in order to maintain conversation context
            for (ChatRequest.Message msg : chatRequest.getMessages()) {
                String role = msg.getRole();
                String content = msg.getContent();
                
                if (content == null || content.isBlank()) {
                    log.warn("Skipping message with empty content (role: {})", role);
                    continue;
                }
                
                // Use fluent API to add messages based on role
                switch (role.toLowerCase()) {
                    case "user":
                        promptSpec = promptSpec.user(content);
                        break;
                    case "system":
                        promptSpec = promptSpec.system(content);
                        break;
                    case "assistant":
                        // Assistant messages are typically handled by conversation memory
                        // but we can add them explicitly if needed
                        log.debug("Assistant message in history (handled by memory advisor)");
                        break;
                    default:
                        log.warn("Unknown message role: {}", role);
                }
            }
            
            // Execute and return content
            String response = promptSpec.call().content();
            log.debug("Chat completion successful, response length: {}", 
                    response != null ? response.length() : 0);
            
            return response;
        });
    }

    @Override
    public <T> Mono<T> getChatCompletionStructured(ChatRequest chatRequest, Class<T> responseType) {
        if (chatRequest == null || chatRequest.getMessages() == null || chatRequest.getMessages().isEmpty()) {
            return Mono.error(new IllegalArgumentException("ChatRequest must contain at least one message"));
        }
        
        if (responseType == null) {
            return Mono.error(new IllegalArgumentException("Response type must not be null"));
        }
        
        return Mono.fromCallable(() -> {
            log.debug("Processing structured chat request with {} messages, target type: {}", 
                    chatRequest.getMessages().size(), responseType.getSimpleName());
            
            // Start building the prompt using fluent API
            var promptSpec = chatClient.prompt();
            
            // Add message history
            for (ChatRequest.Message msg : chatRequest.getMessages()) {
                String role = msg.getRole();
                String content = msg.getContent();
                
                if (content == null || content.isBlank()) {
                    continue;
                }
                
                switch (role.toLowerCase()) {
                    case "user":
                        promptSpec = promptSpec.user(content);
                        break;
                    case "system":
                        promptSpec = promptSpec.system(content);
                        break;
                    default:
                        // Skip other roles
                        break;
                }
            }
            
            // Execute with structured output mapping
            try {
                T result = promptSpec.call().entity(responseType);
                log.debug("Structured output mapping successful to type: {}", 
                        responseType.getSimpleName());
                return result;
            } catch (Exception e) {
                log.error("Failed to map response to type {}: {}", 
                        responseType.getSimpleName(), e.getMessage());
                throw new RuntimeException(
                        String.format("Failed to convert response to %s: %s", 
                                responseType.getSimpleName(), e.getMessage()), 
                        e);
            }
        });
    }

    @Override
    public Flux<String> getChatCompletionStream(ChatRequest chatRequest) {
        if (chatRequest == null || chatRequest.getMessages() == null || chatRequest.getMessages().isEmpty()) {
            return Flux.error(new IllegalArgumentException("ChatRequest must contain at least one message"));
        }
        
        return Flux.defer(() -> {
            log.debug("Processing streaming chat request with {} messages", 
                    chatRequest.getMessages().size());
            
            // Start building the prompt using fluent API
            var promptSpec = chatClient.prompt();
            
            // Add message history
            for (ChatRequest.Message msg : chatRequest.getMessages()) {
                String role = msg.getRole();
                String content = msg.getContent();
                
                if (content == null || content.isBlank()) {
                    continue;
                }
                
                switch (role.toLowerCase()) {
                    case "user":
                        promptSpec = promptSpec.user(content);
                        break;
                    case "system":
                        promptSpec = promptSpec.system(content);
                        break;
                    default:
                        break;
                }
            }
            
            // Execute with streaming
            return promptSpec.stream().content();
        });
    }
}
