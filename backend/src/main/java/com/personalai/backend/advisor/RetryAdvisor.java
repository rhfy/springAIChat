package com.personalai.backend.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Advisor that implements retry logic for transient failures during chat interactions.
 * 
 * This advisor automatically retries failed requests with exponential backoff when:
 * - TimeoutException occurs (network timeout)
 * - IOException occurs (connection issues)
 * 
 * Configuration:
 * - Max attempts: configurable via constructor
 * - Backoff: exponential from 1s to 10s
 * - Retry logging: logs each retry attempt
 * 
 * Requirements: 3.4, 7.5
 */
public class RetryAdvisor implements CallAdvisor {

    private static final Logger log = LoggerFactory.getLogger(RetryAdvisor.class);
    private static final String ADVISOR_NAME = "RetryAdvisor";
    
    private final int maxAttempts;
    private final Duration minBackoff;
    private final Duration maxBackoff;

    /**
     * Creates a RetryAdvisor with specified configuration.
     * 
     * @param maxAttempts maximum number of retry attempts
     * @param minBackoff minimum backoff duration
     * @param maxBackoff maximum backoff duration
     */
    public RetryAdvisor(int maxAttempts, Duration minBackoff, Duration maxBackoff) {
        this.maxAttempts = maxAttempts;
        this.minBackoff = minBackoff;
        this.maxBackoff = maxBackoff;
    }

    @Override
    public String getName() {
        return ADVISOR_NAME;
    }

    @Override
    public int getOrder() {
        // Execute after logging but before memory
        return 100;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxAttempts) {
            try {
                attempt++;
                
                if (attempt > 1) {
                    log.info("Retry attempt {} of {}", attempt, maxAttempts);
                }
                
                // Proceed with the advisor chain
                return callAdvisorChain.nextCall(chatClientRequest);
                
            } catch (Exception e) {
                lastException = e;
                
                // Check if exception is retryable
                if (!isRetryable(e)) {
                    log.error("Non-retryable exception occurred: {}", e.getMessage());
                    throw e;
                }
                
                // If this was the last attempt, throw the exception
                if (attempt >= maxAttempts) {
                    log.error("Max retry attempts ({}) reached, giving up", maxAttempts);
                    throw e;
                }
                
                // Calculate backoff duration with exponential increase
                Duration backoff = calculateBackoff(attempt);
                log.warn("Chat request failed (attempt {}/{}): {}. Retrying in {}ms", 
                        attempt, maxAttempts, e.getMessage(), backoff.toMillis());
                
                // Wait before retrying
                try {
                    Thread.sleep(backoff.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        // This should never be reached, but just in case
        throw new RuntimeException("Retry logic failed", lastException);
    }

    /**
     * Determines if an exception is retryable.
     * 
     * @param e the exception to check
     * @return true if the exception is retryable, false otherwise
     */
    private boolean isRetryable(Exception e) {
        return e instanceof TimeoutException || 
               e instanceof IOException ||
               (e.getCause() instanceof TimeoutException) ||
               (e.getCause() instanceof IOException);
    }

    /**
     * Calculates exponential backoff duration for the given attempt.
     * 
     * @param attempt the current attempt number (1-based)
     * @return the backoff duration
     */
    private Duration calculateBackoff(int attempt) {
        // Exponential backoff: min * 2^(attempt-1), capped at max
        long backoffMillis = minBackoff.toMillis() * (long) Math.pow(2, attempt - 1);
        backoffMillis = Math.min(backoffMillis, maxBackoff.toMillis());
        return Duration.ofMillis(backoffMillis);
    }
}
