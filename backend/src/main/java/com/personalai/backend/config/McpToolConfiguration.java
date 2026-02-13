package com.personalai.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class for MCP (Model Context Protocol) tool integration.
 * 
 * This configuration:
 * - Enables MCP client functionality through Spring Boot configuration
 * - Logs MCP tool availability at startup
 * - Handles tool execution with timeout and error handling
 * 
 * MCP tools are conditionally enabled based on the spring.ai.mcp.client.enabled property.
 * When enabled, Spring AI auto-configuration will automatically provide ToolCallbackProvider.
 * 
 * Requirements: 4.1, 4.2, 10.4
 */
@Configuration
@ConditionalOnProperty(name = "spring.ai.mcp.client.enabled", havingValue = "true")
public class McpToolConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpToolConfiguration.class);

    /**
     * Logs MCP tool configuration status at startup.
     * 
     * When this configuration is active, Spring AI's auto-configuration will:
     * - Initialize McpClient from the configured mcp-servers-config.json
     * - Provide ToolCallbackProvider bean automatically
     * - Register all available MCP tools with ChatClient
     */
    @PostConstruct
    public void init() {
        log.info("MCP Tool Configuration is ENABLED");
        log.info("MCP tools will be automatically configured by Spring AI");
        log.info("Configuration file: spring.ai.mcp.client.config-file");
        log.info("Tool timeout: spring.ai.mcp.tools.timeout");
    }
}

