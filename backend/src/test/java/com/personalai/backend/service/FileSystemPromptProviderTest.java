package com.personalai.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic verification test for FileSystemPromptProvider.
 * This ensures the implementation loads the system prompt correctly.
 */
@SpringBootTest
class FileSystemPromptProviderTest {
    
    @Autowired
    private SystemPromptProvider systemPromptProvider;
    
    @Test
    void shouldLoadSystemPromptSuccessfully() {
        // When: Getting the system prompt
        String prompt = systemPromptProvider.getSystemPrompt();
        
        // Then: Prompt should be loaded and not empty
        assertThat(prompt).isNotNull();
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("helpful AI assistant");
    }
    
    @Test
    void shouldReloadSystemPrompt() {
        // Given: Initial prompt
        String initialPrompt = systemPromptProvider.getSystemPrompt();
        
        // When: Reloading the prompt
        systemPromptProvider.reload();
        String reloadedPrompt = systemPromptProvider.getSystemPrompt();
        
        // Then: Prompt should still be available and consistent
        assertThat(reloadedPrompt).isNotNull();
        assertThat(reloadedPrompt).isNotBlank();
        assertThat(reloadedPrompt).isEqualTo(initialPrompt);
    }
}
