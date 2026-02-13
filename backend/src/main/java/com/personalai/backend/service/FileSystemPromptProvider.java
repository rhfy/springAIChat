package com.personalai.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * File-based implementation of {@link SystemPromptProvider} that loads system prompts
 * from classpath resources.
 * 
 * <p>This implementation loads the system prompt from a configurable file location
 * (default: classpath:system-prompt.md) and caches it in memory for performance.
 * The prompt can be reloaded dynamically using the {@link #reload()} method.</p>
 * 
 * <p>Error Handling:</p>
 * <ul>
 *   <li>If the prompt file cannot be loaded, a fallback prompt is used</li>
 *   <li>Empty or whitespace-only prompts are replaced with the fallback</li>
 *   <li>All errors are logged for debugging</li>
 * </ul>
 * 
 * <p>Configuration:</p>
 * <pre>
 * # application.properties
 * system.prompt.resource=classpath:system-prompt.md
 * </pre>
 * 
 * @see SystemPromptProvider
 */
@Component
@Slf4j
public class FileSystemPromptProvider implements SystemPromptProvider {
    
    /**
     * Default fallback prompt used when the configured prompt cannot be loaded
     * or is empty/whitespace-only.
     */
    private static final String FALLBACK_PROMPT = "You are a helpful AI assistant.";
    
    /**
     * The resource location of the system prompt file.
     * Defaults to classpath:system-prompt.md
     */
    @Value("${system.prompt.resource:classpath:system-prompt.md}")
    private Resource systemPromptResource;
    
    /**
     * Cached system prompt content.
     * This is loaded once at initialization and can be reloaded via {@link #reload()}.
     */
    private String cachedPrompt;
    
    /**
     * Initializes the provider by loading the system prompt at startup.
     * This method is automatically called after dependency injection.
     */
    @PostConstruct
    public void init() {
        reload();
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>Returns the cached system prompt. This method is thread-safe for reads
     * but may return stale content during a concurrent {@link #reload()} operation.</p>
     */
    @Override
    public String getSystemPrompt() {
        return cachedPrompt;
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>Reloads the system prompt from the configured resource. If loading fails
     * or the prompt is empty/whitespace-only, the fallback prompt is used instead.</p>
     * 
     * <p>This method logs the outcome of the reload operation:</p>
     * <ul>
     *   <li>Success: logs prompt length</li>
     *   <li>Empty/whitespace: logs warning and uses fallback</li>
     *   <li>Error: logs error and uses fallback</li>
     * </ul>
     * 
     * <p>Additionally validates that the prompt includes multi-language support instructions.</p>
     */
    @Override
    public void reload() {
        try (InputStreamReader reader = new InputStreamReader(
                systemPromptResource.getInputStream(), StandardCharsets.UTF_8)) {
            
            String loadedPrompt = FileCopyUtils.copyToString(reader);
            
            // Check for empty or whitespace-only content
            if (loadedPrompt == null || loadedPrompt.trim().isEmpty()) {
                log.warn("System prompt file is empty or contains only whitespace. Using fallback prompt.");
                cachedPrompt = FALLBACK_PROMPT;
            } else {
                cachedPrompt = loadedPrompt;
                log.info("System prompt loaded successfully (length: {} characters)", cachedPrompt.length());
                
                // Validate multi-language support instructions
                validateMultiLanguageSupport(cachedPrompt);
            }
            
        } catch (IOException e) {
            log.error("Failed to load system prompt from resource: {}. Using fallback prompt.", 
                    systemPromptResource.getDescription(), e);
            cachedPrompt = FALLBACK_PROMPT;
        }
    }
    
    /**
     * Validates that the system prompt includes multi-language support instructions.
     * 
     * <p>This method checks for keywords indicating language-matching behavior:
     * "same language", "language as the user", etc.</p>
     * 
     * <p>If validation fails, a warning is logged but the prompt is still used.
     * This is a soft validation to ensure best practices without breaking functionality.</p>
     * 
     * @param prompt the system prompt to validate
     */
    private void validateMultiLanguageSupport(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        boolean hasLanguageInstructions = 
                lowerPrompt.contains("same language") ||
                lowerPrompt.contains("language as the user") ||
                lowerPrompt.contains("answer in") ||
                lowerPrompt.contains("respond in");
        
        if (hasLanguageInstructions) {
            log.info("Multi-language support instructions detected in system prompt");
        } else {
            log.warn("System prompt may not include multi-language support instructions. " +
                    "Consider adding instructions to match the user's language.");
        }
    }
}
