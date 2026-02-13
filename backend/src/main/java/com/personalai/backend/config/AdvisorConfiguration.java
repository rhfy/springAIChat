package com.personalai.backend.config;

import com.personalai.backend.advisor.LoggingAdvisor;
import com.personalai.backend.advisor.ObservationAdvisor;
import com.personalai.backend.advisor.RetryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Spring AI Advisors.
 * 
 * Advisors implement cross-cutting concerns for chat interactions including:
 * - Conversation memory management
 * - Request/response logging
 * - Retry logic for transient failures
 * - Observability and tracing
 * 
 * Each advisor can be enabled/disabled via application properties.
 */
@Configuration
public class AdvisorConfiguration {

    /**
     * Provides in-memory chat memory repository for storing conversation history.
     * This bean is created only if no other ChatMemoryRepository bean is defined.
     * 
     * @return InMemoryChatMemoryRepository instance for storing messages
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * Provides chat memory implementation with message window management.
     * Stores a configurable number of recent messages per conversation.
     * 
     * @param chatMemoryRepository the repository for storing messages
     * @param maxMessages maximum number of messages to retain per conversation
     * @return ChatMemory instance configured with message window
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.chat.memory.enabled", havingValue = "true", matchIfMissing = true)
    public ChatMemory chatMemory(
            ChatMemoryRepository chatMemoryRepository,
            @Value("${spring.ai.chat.memory.max-messages:20}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    /**
     * Provides conversation memory management through MessageChatMemoryAdvisor.
     * Automatically maintains chat history across multiple requests using the configured ChatMemory.
     * 
     * This advisor is conditionally created based on the spring.ai.chat.memory.enabled property.
     * When enabled, it preserves message order and context without manual list building.
     * 
     * @param chatMemory the chat memory implementation for storing conversation history
     * @return MessageChatMemoryAdvisor instance for automatic conversation history management
     * @see ChatMemory
     * @see MessageWindowChatMemory
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.chat.memory.enabled", havingValue = "true", matchIfMissing = true)
    public org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor messageChatMemoryAdvisor(
            ChatMemory chatMemory) {
        return org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor.builder(chatMemory)
                .build();
    }

    /**
     * Provides request/response logging for all chat interactions.
     * Logs request details (user message, timestamp) and response details
     * (assistant message, model used, duration).
     * 
     * @return LoggingAdvisor instance for capturing chat interaction logs
     */
    @Bean
    public LoggingAdvisor loggingAdvisor() {
        return new LoggingAdvisor();
    }

    /**
     * Provides retry logic for transient failures during chat interactions.
     * Automatically retries failed requests with exponential backoff.
     * 
     * This advisor is conditionally created based on the spring.ai.chat.retry.enabled property.
     * When enabled, it retries on TimeoutException and IOException with configurable max attempts.
     * 
     * @param maxAttempts maximum number of retry attempts from configuration
     * @return RetryAdvisor instance configured with retry policy
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.chat.retry.enabled", havingValue = "true")
    public RetryAdvisor retryAdvisor(
            @Value("${spring.ai.chat.retry.max-attempts:3}") int maxAttempts) {
        return new RetryAdvisor(
                maxAttempts,
                java.time.Duration.ofSeconds(1),
                java.time.Duration.ofSeconds(10)
        );
    }

    /**
     * Provides observability integration for chat interactions.
     * Emits trace events containing request ID, duration, model used, and outcome status.
     * 
     * This advisor is conditionally created based on the management.tracing.enabled property.
     * When enabled, it integrates with Spring Boot Actuator's ObservationRegistry.
     * 
     * @param observationRegistry the observation registry for emitting trace events
     * @return ObservationAdvisor instance for observability integration
     */
    @Bean
    @ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true")
    public ObservationAdvisor observationAdvisor(
            io.micrometer.observation.ObservationRegistry observationRegistry) {
        return new ObservationAdvisor(observationRegistry);
    }

    // All advisor beans have been configured
}
