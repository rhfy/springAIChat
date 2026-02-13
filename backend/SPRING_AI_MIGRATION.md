# Spring AI 1.1.2 Migration Guide

## Overview

This document describes the migration from manual Prompt construction to Spring AI 1.1.2 best practices using ChatClient's fluent API, Advisors, and enhanced features.

## What Changed

### Before (Legacy GroqService)
- Manual Prompt construction with ArrayList
- Manual message list building
- No advisor support
- Limited observability
- Tightly coupled to Groq API

### After (ChatServiceImpl)
- ChatClient fluent API
- Automatic conversation memory
- Advisor chain (logging, retry, memory, observability)
- Enhanced error handling
- Better testability
- Structured output support
- Streaming responses

## Architecture

```
ChatController
    ↓
ChatService (interface)
    ↓
ChatServiceImpl
    ↓
ChatClient (Spring AI)
    ↓
Advisor Chain
    ├── LoggingAdvisor
    ├── RetryAdvisor
    ├── MessageChatMemoryAdvisor
    └── ObservationAdvisor
    ↓
Groq API (OpenAI-compatible)
```

## Configuration

### application.properties

```properties
# Spring AI - Groq API Configuration
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.api-key=${GROQ_API_KEY}
spring.ai.openai.chat.options.model=llama-3.3-70b-versatile
spring.ai.openai.chat.options.temperature=0.7
spring.ai.openai.chat.options.max-tokens=2000

# Conversation Memory
spring.ai.chat.memory.enabled=true
spring.ai.chat.memory.max-messages=20

# Retry Configuration
spring.ai.chat.retry.enabled=true
spring.ai.chat.retry.max-attempts=3

# Observability
management.tracing.enabled=true
management.metrics.export.simple.enabled=true
management.endpoints.web.exposure.include=health,metrics,info

# MCP Tools (optional)
spring.ai.mcp.client.enabled=false
spring.ai.mcp.client.config-file=classpath:mcp-servers-config.json
spring.ai.mcp.tools.timeout=30s
```

### Environment Variables

Required:
- `GROQ_API_KEY`: Your Groq API key

Optional:
- `SPRING_AI_OPENAI_BASE_URL`: Override default base URL
- `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL`: Override default model

## Features

### 1. ChatClient Fluent API

**Before:**
```java
List<Message> messages = new ArrayList<>();
messages.add(new SystemMessage(systemPrompt));
messages.add(new UserMessage(userMessage));
Prompt prompt = new Prompt(messages);
ChatResponse response = chatClient.call(prompt);
```

**After:**
```java
String response = chatClient.prompt()
    .user(userMessage)
    .call()
    .content();
```

### 2. Conversation Memory

Automatically maintains chat history across requests:

```java
// First request
chatService.getChatCompletion(request1); // "What is Spring AI?"

// Second request - context is preserved
chatService.getChatCompletion(request2); // "Tell me more about it"
// AI remembers the previous question about Spring AI
```

Configuration:
```properties
spring.ai.chat.memory.enabled=true
spring.ai.chat.memory.max-messages=20
```

### 3. Advisors

#### LoggingAdvisor
Logs all requests and responses with timing information.

```
INFO  - Chat request received at 2024-01-15T10:30:00Z
DEBUG - User message: Hello
INFO  - Chat response generated in 1234ms using model: llama-3.3-70b-versatile
DEBUG - Assistant message: Hi! How can I help you?
```

#### RetryAdvisor
Automatically retries failed requests with exponential backoff.

```properties
spring.ai.chat.retry.enabled=true
spring.ai.chat.retry.max-attempts=3
```

Retries on:
- TimeoutException
- IOException
- Network errors

#### MessageChatMemoryAdvisor
Manages conversation history automatically.

```properties
spring.ai.chat.memory.enabled=true
spring.ai.chat.memory.max-messages=20
```

#### ObservationAdvisor
Emits metrics and traces for monitoring.

```properties
management.tracing.enabled=true
```

Metrics include:
- Request duration
- Token usage
- Model used
- Success/failure status

### 4. Structured Output

Map AI responses to Java objects:

```java
public record WeatherResponse(String location, double temperature, String condition) {}

WeatherResponse weather = chatService.getChatCompletionStructured(
    request, 
    WeatherResponse.class
).block();
```

### 5. Streaming Responses

Real-time response streaming:

```java
Flux<String> stream = chatService.getChatCompletionStream(request);
stream.subscribe(chunk -> System.out.print(chunk));
```

REST endpoint:
```
POST /api/chat/stream
```

### 6. MCP Tool Integration

Enable external tools via Model Context Protocol:

```properties
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.config-file=classpath:mcp-servers-config.json
```

Tools are automatically registered and available to the AI model.

### 7. Dynamic System Prompt Reload

Update system prompt without restart:

```
POST /api/admin/reload-prompt
```

Response:
```json
{
  "status": "success",
  "message": "System prompt reloaded successfully",
  "promptLength": 383
}
```

### 8. Health Checks

Custom health indicator for ChatClient:

```
GET /actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "chatClient": {
      "status": "UP",
      "details": {
        "responseTime": "1234ms",
        "responseLength": 42
      }
    }
  }
}
```

## API Endpoints

### Chat Completion
```
POST /api/chat
Content-Type: application/json

{
  "messages": [
    {"role": "user", "content": "Hello"}
  ]
}
```

Response:
```json
{
  "message": {
    "role": "assistant",
    "content": "Hi! How can I help you?"
  }
}
```

### Streaming Chat
```
POST /api/chat/stream
Content-Type: application/json

{
  "messages": [
    {"role": "user", "content": "Tell me a story"}
  ]
}
```

Returns Server-Sent Events (SSE) stream.

### Admin Endpoints

#### Reload System Prompt
```
POST /api/admin/reload-prompt
```

#### Get Current System Prompt
```
GET /api/admin/system-prompt
```

### Actuator Endpoints

```
GET /actuator/health
GET /actuator/metrics
GET /actuator/info
```

## Migration Steps

### 1. Update Dependencies

Already included in `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation "org.springframework.ai:spring-ai-starter-model-openai:1.1.2"
```

### 2. Update Configuration

Add new properties to `application.properties` (see Configuration section above).

### 3. Replace Service Injection

**Before:**
```java
@Autowired
private GroqService groqService;
```

**After:**
```java
@Autowired
private ChatService chatService;
```

### 4. Update Method Calls

The API remains the same:
```java
Mono<String> response = chatService.getChatCompletion(chatRequest);
```

### 5. Test

Run the application and verify:
- Chat requests work correctly
- Conversation memory is preserved
- Advisors are logging properly
- Health checks return UP status

## Troubleshooting

### Issue: API Key Not Found

**Error:** `Spring AI API key must not be blank`

**Solution:** Set the `GROQ_API_KEY` environment variable:
```bash
export GROQ_API_KEY=your-api-key-here
```

### Issue: Conversation Memory Not Working

**Check:**
1. `spring.ai.chat.memory.enabled=true` in application.properties
2. MessageChatMemoryAdvisor bean is created (check logs)
3. Using the same conversation ID across requests

### Issue: Retries Not Working

**Check:**
1. `spring.ai.chat.retry.enabled=true` in application.properties
2. RetryAdvisor bean is created (check logs)
3. Error is retryable (TimeoutException, IOException)

### Issue: Health Check Fails

**Possible causes:**
1. API key is invalid
2. Network connectivity issues
3. Groq API is down
4. Rate limiting

**Check logs for detailed error messages.**

### Issue: MCP Tools Not Loading

**Check:**
1. `spring.ai.mcp.client.enabled=true`
2. `mcp-servers-config.json` exists and is valid
3. MCP servers are running and accessible
4. Check logs for MCP initialization errors

## Performance Considerations

### Token Usage

Monitor token usage through:
- Logs (DEBUG level shows token counts)
- Observability metrics
- Actuator metrics endpoint

### Rate Limiting

Groq API has rate limits. The RetryAdvisor helps handle transient failures, but persistent rate limiting requires:
- Reducing request frequency
- Implementing request queuing
- Using a higher tier API plan

### Memory Usage

Conversation memory stores messages in-memory. For production:
- Consider persistent storage (database, Redis)
- Implement conversation cleanup/expiration
- Monitor memory usage

## Best Practices

1. **Always use environment variables for API keys**
   - Never commit API keys to version control
   - Use `.env` files for local development

2. **Enable observability in production**
   - Set `management.tracing.enabled=true`
   - Integrate with monitoring tools (Prometheus, Grafana)

3. **Configure appropriate timeouts**
   - MCP tools: `spring.ai.mcp.tools.timeout=30s`
   - HTTP client timeouts in WebClient configuration

4. **Implement proper error handling**
   - Use reactive error handling with `onErrorResume`
   - Provide meaningful error messages to users
   - Log errors with sufficient context

5. **Test with property-based testing**
   - Use jqwik for property-based tests
   - Test conversation memory preservation
   - Test retry behavior
   - Test structured output mapping

6. **Secure admin endpoints**
   - Add Spring Security
   - Implement role-based access control
   - Restrict `/api/admin/*` to admin users only

## Additional Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Groq API Documentation](https://console.groq.com/docs)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

## Support

For issues or questions:
1. Check this documentation
2. Review application logs
3. Check Spring AI documentation
4. Open an issue in the project repository
