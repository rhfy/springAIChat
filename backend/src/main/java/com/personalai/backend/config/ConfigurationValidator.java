package com.personalai.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validates Spring AI configuration at application startup.
 * 
 * This component:
 * - Performs additional validation beyond @Validated annotations
 * - Logs configuration status for debugging
 * - Fails fast with clear error messages for invalid configuration
 * 
 * Validation is performed after dependency injection but before the application
 * starts accepting requests.
 * 
 * Requirements: 10.5
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationValidator {

    private final SpringAiConfigurationProperties springAiConfig;

    /**
     * Validates configuration at startup.
     * 
     * This method is automatically called after dependency injection.
     * If validation fails, the application will not start.
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("Validating Spring AI configuration...");
        
        try {
            // Validate base URL format
            validateBaseUrl();
            
            // Validate API key
            validateApiKey();
            
            // Validate model configuration
            validateModelConfiguration();
            
            // Log successful validation
            logConfigurationSummary();
            
            log.info("Spring AI configuration validation PASSED");
            
        } catch (IllegalStateException e) {
            log.error("Spring AI configuration validation FAILED: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validates the base URL format.
     */
    private void validateBaseUrl() {
        String baseUrl = springAiConfig.getBaseUrl();
        
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalStateException(
                    "Invalid base URL: " + baseUrl + ". Must start with http:// or https://");
        }
        
        log.debug("Base URL validation passed: {}", baseUrl);
    }

    /**
     * Validates the API key.
     */
    private void validateApiKey() {
        String apiKey = springAiConfig.getApiKey();
        
        // Check if using default/placeholder key
        if (apiKey.startsWith("your-") || apiKey.equals("REPLACE_ME")) {
            throw new IllegalStateException(
                    "API key appears to be a placeholder. Please set a valid GROQ_API_KEY environment variable.");
        }
        
        // Check minimum length (most API keys are at least 20 characters)
        if (apiKey.length() < 20) {
            log.warn("API key seems unusually short (length: {}). This may indicate an invalid key.", 
                    apiKey.length());
        }
        
        log.debug("API key validation passed (length: {})", apiKey.length());
    }

    /**
     * Validates model configuration.
     */
    private void validateModelConfiguration() {
        var options = springAiConfig.getChat().getOptions();
        
        String model = options.getModel();
        Double temperature = options.getTemperature();
        Integer maxTokens = options.getMaxTokens();
        
        // Validate temperature range
        if (temperature != null && (temperature < 0.0 || temperature > 2.0)) {
            throw new IllegalStateException(
                    "Invalid temperature: " + temperature + ". Must be between 0.0 and 2.0");
        }
        
        // Validate max tokens
        if (maxTokens != null && maxTokens > 100000) {
            log.warn("Max tokens is very high ({}). This may result in high costs.", maxTokens);
        }
        
        log.debug("Model configuration validation passed: model={}, temperature={}, maxTokens={}", 
                model, temperature, maxTokens);
    }

    /**
     * Logs a summary of the validated configuration.
     */
    private void logConfigurationSummary() {
        var options = springAiConfig.getChat().getOptions();
        
        log.info("=== Spring AI Configuration Summary ===");
        log.info("Base URL: {}", springAiConfig.getBaseUrl());
        log.info("API Key: {}...{} (length: {})", 
                springAiConfig.getApiKey().substring(0, Math.min(4, springAiConfig.getApiKey().length())),
                springAiConfig.getApiKey().substring(Math.max(0, springAiConfig.getApiKey().length() - 4)),
                springAiConfig.getApiKey().length());
        log.info("Model: {}", options.getModel());
        log.info("Temperature: {}", options.getTemperature());
        log.info("Max Tokens: {}", options.getMaxTokens());
        log.info("======================================");
    }
}
