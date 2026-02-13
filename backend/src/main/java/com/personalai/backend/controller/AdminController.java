package com.personalai.backend.controller;

import com.personalai.backend.service.SystemPromptProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for administrative operations.
 * 
 * This controller provides endpoints for:
 * - Dynamic system prompt reloading
 * - Configuration management
 * - System health checks
 * 
 * Security Note: In production, these endpoints should be restricted to admin users only.
 * Consider adding Spring Security with role-based access control.
 * 
 * Requirements: 6.3
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final SystemPromptProvider systemPromptProvider;

    /**
     * Reload the system prompt from its source.
     * 
     * This endpoint:
     * - Triggers SystemPromptProvider.reload()
     * - Updates the cached system prompt
     * - Subsequent chat requests will use the new prompt
     * - Does not require service restart
     * 
     * Security: This endpoint should be restricted to admin users in production.
     * 
     * @return ResponseEntity with success message or error
     */
    @PostMapping("/reload-prompt")
    public ResponseEntity<Object> reloadSystemPrompt() {
        log.info("Admin request: Reloading system prompt");
        
        try {
            systemPromptProvider.reload();
            String currentPrompt = systemPromptProvider.getSystemPrompt();
            
            log.info("System prompt reloaded successfully (length: {})", currentPrompt.length());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "System prompt reloaded successfully",
                    "promptLength", currentPrompt.length()
            ));
        } catch (Exception e) {
            log.error("Failed to reload system prompt: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to reload system prompt: " + e.getMessage()
                    ));
        }
    }

    /**
     * Get the current system prompt.
     * 
     * This endpoint returns the currently loaded system prompt for inspection.
     * Useful for debugging and verification.
     * 
     * Security: This endpoint should be restricted to admin users in production.
     * 
     * @return ResponseEntity with current system prompt
     */
    @GetMapping("/system-prompt")
    public ResponseEntity<Object> getSystemPrompt() {
        log.debug("Admin request: Get current system prompt");
        
        try {
            String currentPrompt = systemPromptProvider.getSystemPrompt();
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "prompt", currentPrompt,
                    "length", currentPrompt.length()
            ));
        } catch (Exception e) {
            log.error("Failed to get system prompt: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to get system prompt: " + e.getMessage()
                    ));
        }
    }
}
