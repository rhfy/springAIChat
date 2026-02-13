package com.personalai.backend.service;

/**
 * Interface for managing system prompts in the AI chat system.
 * 
 * <p>System prompts define the AI assistant's behavior, constraints, and instructions.
 * Implementations should handle loading, caching, and reloading of system prompt content
 * from various sources (files, databases, configuration, etc.).</p>
 * 
 * <p>This interface supports dynamic prompt updates without requiring service restarts,
 * enabling runtime configuration changes for the AI assistant's behavior.</p>
 * 
 * @see FileSystemPromptProvider
 */
public interface SystemPromptProvider {
    
    /**
     * Retrieves the current system prompt text.
     * 
     * <p>This method should return a cached version of the prompt for performance.
     * The prompt defines the AI assistant's behavior, language preferences, and
     * operational constraints.</p>
     * 
     * @return the system prompt text, never null
     */
    String getSystemPrompt();
    
    /**
     * Reloads the system prompt from its source.
     * 
     * <p>This method allows dynamic updates to the system prompt without requiring
     * a service restart. Implementations should handle errors gracefully and may
     * fall back to a default prompt if reloading fails.</p>
     * 
     * <p>After calling this method, subsequent calls to {@link #getSystemPrompt()}
     * should return the newly loaded prompt.</p>
     */
    void reload();
}
