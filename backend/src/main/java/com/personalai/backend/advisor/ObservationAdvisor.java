package com.personalai.backend.advisor;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
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
import java.util.UUID;

/**
 * Advisor that provides observability integration for chat interactions.
 * 
 * This advisor emits trace events containing:
 * - Request ID (unique identifier for each request)
 * - Duration (time taken to process the request)
 * - Model used (AI model name)
 * - Token count (prompt, completion, and total tokens)
 * - Outcome status (success or error)
 * 
 * Integrates with Spring Boot Actuator's ObservationRegistry for metrics and tracing.
 * 
 * Requirements: 7.2, 7.3
 */
public class ObservationAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ObservationAdvisor.class);
    private static final String ADVISOR_NAME = "ObservationAdvisor";
    private static final String OBSERVATION_NAME = "spring.ai.chat";
    
    private final ObservationRegistry observationRegistry;

    /**
     * Creates an ObservationAdvisor with the specified observation registry.
     * 
     * @param observationRegistry the registry for emitting observations
     */
    public ObservationAdvisor(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Override
    public String getName() {
        return ADVISOR_NAME;
    }

    @Override
    public int getOrder() {
        // Execute early to capture all metrics
        return 50;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        String requestId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        // Create observation for this chat interaction
        Observation observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
                .lowCardinalityKeyValue("request.id", requestId)
                .lowCardinalityKeyValue("type", "call");
        
        return observation.observe(() -> {
            try {
                // Proceed with the advisor chain
                ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
                
                // Calculate duration
                Duration duration = Duration.between(startTime, Instant.now());
                
                // Extract metadata from response
                recordSuccessMetrics(observation, response, duration);
                
                return response;
            } catch (Exception e) {
                Duration duration = Duration.between(startTime, Instant.now());
                recordErrorMetrics(observation, e, duration);
                throw e;
            }
        });
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        String requestId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();
        
        // Create observation for this streaming chat interaction
        Observation observation = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
                .lowCardinalityKeyValue("request.id", requestId)
                .lowCardinalityKeyValue("type", "stream");
        
        observation.start();
        
        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnComplete(() -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    observation.highCardinalityKeyValue("duration.ms", String.valueOf(duration.toMillis()));
                    observation.lowCardinalityKeyValue("outcome", "success");
                    observation.stop();
                    log.debug("Observation recorded for streaming request {}: success in {}ms", 
                            requestId, duration.toMillis());
                })
                .doOnError(e -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    observation.highCardinalityKeyValue("duration.ms", String.valueOf(duration.toMillis()));
                    observation.lowCardinalityKeyValue("outcome", "error");
                    observation.highCardinalityKeyValue("error.type", e.getClass().getSimpleName());
                    observation.error(e);
                    observation.stop();
                    log.debug("Observation recorded for streaming request {}: error after {}ms", 
                            requestId, duration.toMillis());
                });
    }

    /**
     * Records success metrics in the observation.
     * 
     * @param observation the observation to record metrics in
     * @param response the chat response
     * @param duration the duration of the request
     */
    private void recordSuccessMetrics(Observation observation, ChatClientResponse response, Duration duration) {
        observation.highCardinalityKeyValue("duration.ms", String.valueOf(duration.toMillis()));
        observation.lowCardinalityKeyValue("outcome", "success");
        
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse != null) {
            // Record model name
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getModel() != null) {
                observation.lowCardinalityKeyValue("model", chatResponse.getMetadata().getModel());
            }
            
            // Record token usage
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                observation.highCardinalityKeyValue("tokens.prompt", 
                        String.valueOf(chatResponse.getMetadata().getUsage().getPromptTokens()));
                observation.highCardinalityKeyValue("tokens.completion", 
                        String.valueOf(chatResponse.getMetadata().getUsage().getCompletionTokens()));
                observation.highCardinalityKeyValue("tokens.total", 
                        String.valueOf(chatResponse.getMetadata().getUsage().getTotalTokens()));
            }
        }
        
        log.debug("Observation recorded: success in {}ms", duration.toMillis());
    }

    /**
     * Records error metrics in the observation.
     * 
     * @param observation the observation to record metrics in
     * @param error the error that occurred
     * @param duration the duration before the error
     */
    private void recordErrorMetrics(Observation observation, Exception error, Duration duration) {
        observation.highCardinalityKeyValue("duration.ms", String.valueOf(duration.toMillis()));
        observation.lowCardinalityKeyValue("outcome", "error");
        observation.highCardinalityKeyValue("error.type", error.getClass().getSimpleName());
        observation.error(error);
        
        log.debug("Observation recorded: error after {}ms - {}", duration.toMillis(), error.getMessage());
    }
}
