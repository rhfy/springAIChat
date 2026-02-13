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
                boolean modified = false;
                
                // Check if extra_body exists and remove it
                if (jsonNode.has("extra_body")) {
                    log.debug("Removing 'extra_body' parameter from request to Groq API");
                    ((ObjectNode) jsonNode).remove("extra_body");
                    modified = true;
                }
                
                // Process messages to remove null values from tool call arguments
                // This is necessary because Groq API doesn't accept null values for optional parameters
                if (jsonNode.has("messages")) {
                    JsonNode messages = jsonNode.get("messages");
                    if (messages.isArray()) {
                        for (JsonNode message : messages) {
                            if (message.has("tool_calls")) {
                                JsonNode toolCalls = message.get("tool_calls");
                                if (toolCalls.isArray()) {
                                    for (JsonNode toolCall : toolCalls) {
                                        if (toolCall.has("function") && toolCall.get("function").has("arguments")) {
                                            String argumentsStr = toolCall.get("function").get("arguments").asText();
                                            try {
                                                JsonNode argumentsJson = objectMapper.readTree(argumentsStr);
                                                JsonNode cleanedArguments = removeNullValues(argumentsJson);
                                                String cleanedArgumentsStr = objectMapper.writeValueAsString(cleanedArguments);
                                                ((ObjectNode) toolCall.get("function")).put("arguments", cleanedArgumentsStr);
                                                modified = true;
                                                log.debug("Cleaned null values from tool call arguments");
                                            } catch (Exception e) {
                                                log.warn("Failed to clean tool call arguments: {}", e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (modified) {
                    // Convert back to bytes
                    String modifiedBody = objectMapper.writeValueAsString(jsonNode);
                    body = modifiedBody.getBytes(StandardCharsets.UTF_8);
                    log.trace("Modified request body: {}", modifiedBody);
                }
            } catch (Exception e) {
                log.warn("Failed to process request body: {}", e.getMessage());
                // Continue with original body if parsing fails
            }
        }

        return execution.execute(request, body);
    }
    
    /**
     * Recursively removes properties with null values from JSON.
     * This is necessary because Groq API doesn't accept null values for optional parameters.
     */
    private JsonNode removeNullValues(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            ObjectNode result = objectMapper.createObjectNode();
            
            java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                java.util.Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                
                if (!value.isNull()) {
                    if (value.isObject() || value.isArray()) {
                        result.set(field.getKey(), removeNullValues(value));
                    } else {
                        result.set(field.getKey(), value);
                    }
                }
            }
            return result;
        } else if (node.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                if (!element.isNull()) {
                    arrayNode.add(removeNullValues(element));
                }
            }
            return arrayNode;
        }
        return node;
    }
}
