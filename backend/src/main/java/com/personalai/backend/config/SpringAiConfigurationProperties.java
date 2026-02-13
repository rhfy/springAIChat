package com.personalai.backend.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Spring AI settings.
 * 
 * This class:
 * - Binds Spring AI configuration from application.properties
 * - Validates configuration at startup with fail-fast behavior
 * - Provides type-safe access to configuration values
 * 
 * Validation ensures:
 * - API key is not blank
 * - Model name is not blank
 * - Numeric values are within valid ranges
 * 
 * If validation fails, the application will not start and will log clear error messages.
 * 
 * Requirements: 10.5
 */
@Configuration
@ConfigurationProperties(prefix = "spring.ai.openai")
@Validated
@Data
public class SpringAiConfigurationProperties {

    /**
     * Base URL for the OpenAI-compatible API endpoint.
     * Example: https://api.groq.com/openai
     */
    @NotBlank(message = "Spring AI base URL must not be blank")
    private String baseUrl;

    /**
     * API key for authentication with the AI service.
     * Should be provided via environment variable or secure configuration.
     */
    @NotBlank(message = "Spring AI API key must not be blank. Set GROQ_API_KEY environment variable.")
    private String apiKey;

    /**
     * Chat-specific configuration options.
     */
    private ChatOptions chat = new ChatOptions();

    /**
     * Chat configuration options including model and generation parameters.
     */
    @Data
    public static class ChatOptions {
        
        /**
         * Options for chat model behavior.
         */
        private Options options = new Options();

        /**
         * Model-specific options.
         */
        @Data
        public static class Options {
            
            /**
             * Name of the AI model to use.
             * Example: llama-3.3-70b-versatile
             */
            @NotBlank(message = "Spring AI model name must not be blank")
            private String model;

            /**
             * Temperature for response generation (0.0 to 2.0).
             * Higher values make output more random, lower values more deterministic.
             */
            @Min(value = 0, message = "Temperature must be >= 0")
            private Double temperature = 0.7;

            /**
             * Maximum number of tokens to generate in the response.
             */
            @Min(value = 1, message = "Max tokens must be >= 1")
            private Integer maxTokens = 2000;
        }
    }
}
