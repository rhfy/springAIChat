package com.personalai.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HTTP request interceptor that removes the 'extra_body' parameter from requests.
 * 
 * This is a workaround for Groq API compatibility with Spring AI 1.1.2.
 * Groq API does not support the 'extra_body' parameter that Spring AI sends,
 * so we intercept the request and remove it before sending to Groq.
 */
public class GroqRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GroqRequestInterceptor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // Only process POST requests to chat completions endpoint
        if ("POST".equals(request.getMethod().name()) && 
            request.getURI().getPath().contains("/chat/completions")) {
            
            try {
                // Parse the request body as JSON
                String bodyString = new String(body, StandardCharsets.UTF_8);
                JsonNode jsonNode = objectMapper.readTree(bodyString);
                
                // Check if extra_body exists and remove it
                if (jsonNode.has("extra_body")) {
                    log.debug("Removing 'extra_body' parameter from request to Groq API");
                    ((ObjectNode) jsonNode).remove("extra_body");
                    
                    // Convert back to bytes
                    String modifiedBody = objectMapper.writeValueAsString(jsonNode);
                    body = modifiedBody.getBytes(StandardCharsets.UTF_8);
                    
                    log.trace("Modified request body: {}", modifiedBody);
                }
            } catch (Exception e) {
                log.warn("Failed to process request body for extra_body removal: {}", e.getMessage());
                // Continue with original body if parsing fails
            }
        }

        return execution.execute(request, body);
    }
}
