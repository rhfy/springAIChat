# Implementation Plan: Spring AI 1.1.2 Migration

## Overview

This implementation plan breaks down the Spring AI migration into discrete, incremental coding tasks. The migration follows a phased approach to maintain backward compatibility while progressively adopting Spring AI best practices. Each task builds on previous work, with checkpoints to validate functionality before proceeding.

The plan prioritizes non-breaking changes first, then gradually refactors the service layer to use ChatClient's fluent API, introduces Advisors for cross-cutting concerns, and enhances MCP tool integration. Testing tasks are integrated throughout to catch issues early.

## Tasks

- [ ] 1. Set up foundation and configuration infrastructure
  - [x] 1.1 Add jqwik dependency for property-based testing
    - Add `testImplementation 'net.jqwik:jqwik:1.8.2'` to build.gradle
    - Configure jqwik with JUnit 5 platform
    - _Requirements: 11.1, 11.2_
  
  - [x] 1.2 Create SystemPromptProvider interface and implementation
    - Create `SystemPromptProvider` interface with `getSystemPrompt()` and `reload()` methods
    - Implement `FileSystemPromptProvider` that loads from classpath resource
    - Add caching and error handling with fallback prompt
    - _Requirements: 6.1, 6.3, 6.5_
  
  - [ ]* 1.3 Write unit test for system prompt loading (Example 4)
    - Test successful loading from file
    - Test fallback when file is missing
    - Test edge case: empty or whitespace-only prompt
    - _Requirements: 6.1, 6.5_
  
  - [x] 1.4 Create AdvisorConfiguration class with basic structure
    - Create `@Configuration` class for advisor beans
    - Add conditional bean creation based on properties
    - Set up configuration properties in application.properties
    - _Requirements: 3.1, 10.1, 10.2_
  
  - [x] 1.5 Update application.properties with Spring AI configuration
    - Add conversation memory settings (`spring.ai.chat.memory.enabled=true`)
    - Add retry configuration (`spring.ai.chat.retry.enabled=true`, `max-attempts=3`)
    - Add observability settings (`management.tracing.enabled=true`)
    - Add logging levels for Spring AI and application packages
    - _Requirements: 2.4, 3.4, 7.2, 10.1_

- [ ] 2. Implement Advisor components
  - [x] 2.1 Create LoggingAdvisor for request/response logging
    - Implement advisor that logs request details (user message, timestamp)
    - Log response details (assistant message, model used, duration)
    - Use SLF4J with appropriate log levels
    - _Requirements: 3.5, 7.4, 7.5_
  
  - [ ]* 2.2 Write property test for request/response logging (Property 4)
    - **Property 4: Request/Response Logging**
    - Generate random chat requests and verify log entries are created
    - **Validates: Requirements 3.5**
  
  - [x] 2.3 Create MessageChatMemoryAdvisor configuration
    - Configure `MessageChatMemoryAdvisor` bean with `InMemoryChatMemory`
    - Set max messages limit from configuration
    - Add conditional creation based on `spring.ai.chat.memory.enabled`
    - _Requirements: 2.2, 2.4, 2.5_
  
  - [x] 2.4 Create RetryAdvisor configuration
    - Configure `RetryAdvisor` with max attempts from properties
    - Set exponential backoff (1s to 10s)
    - Configure retry on TimeoutException and IOException
    - Add retry logging callback
    - _Requirements: 3.4, 7.5_
  
  - [ ]* 2.5 Write property test for retry on transient failures (Property 3)
    - **Property 3: Retry on Transient Failures**
    - Simulate transient failures and verify retry attempts
    - **Validates: Requirements 3.4**
  
  - [x] 2.6 Create ObservationAdvisor configuration
    - Configure `ObservationAdvisor` with `ObservationRegistry`
    - Add conditional creation based on `management.tracing.enabled`
    - _Requirements: 7.2, 7.3_
  
  - [ ]* 2.7 Write property test for observability event emission (Property 10)
    - **Property 10: Observability Event Emission**
    - Generate random chat requests and verify trace events are emitted
    - **Validates: Requirements 7.3**

- [x] 3. Checkpoint - Verify advisor infrastructure
  - Ensure all advisor beans are created correctly
  - Verify configuration properties are loaded
  - Run existing tests to ensure no regressions
  - Ask the user if questions arise

- [ ] 4. Create new ChatService implementation with fluent API
  - [x] 4.1 Create ChatService interface
    - Define `getChatCompletion(ChatRequest)` returning `Mono<String>`
    - Define `getChatCompletionStructured(ChatRequest, Class<T>)` returning `Mono<T>`
    - Add JavaDoc comments explaining parameters and return types
    - _Requirements: 1.1, 5.1, 12.1_
  
  - [x] 4.2 Implement ChatServiceImpl with ChatClient fluent API
    - Inject `ChatClient.Builder`, `SystemPromptProvider`, `List<Advisor>`, `Optional<ToolCallbackProvider>`
    - Build ChatClient in constructor with `defaultSystem()`, `defaultAdvisors()`, `defaultToolCallbacks()`
    - Implement `getChatCompletion()` using fluent API: `chatClient.prompt().user().call().content()`
    - Process message history by chaining `.user()` and `.system()` calls
    - Wrap in `Mono.fromCallable()` for reactive support
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 2.1, 8.1, 8.2_
  
  - [x] 4.3 Implement structured output support in ChatServiceImpl
    - Implement `getChatCompletionStructured()` using `.entity(responseType)`
    - Add error handling for conversion failures with clear messages
    - _Requirements: 5.1, 5.3, 5.4_
  
  - [ ]* 4.4 Write unit test for service initialization (Example 1)
    - Test ChatClient initializes successfully with default configuration
    - Verify service can process simple requests
    - **Validates: Requirements 1.5**
  
  - [ ]* 4.5 Write property test for conversation memory preservation (Property 1)
    - **Property 1: Conversation Memory Preservation**
    - Generate random message sequences and verify context is maintained
    - **Validates: Requirements 2.1, 2.3, 2.5**
  
  - [ ]* 4.6 Write property test for advisor application (Property 2)
    - **Property 2: Advisor Application**
    - Verify advisors are applied automatically to all requests
    - **Validates: Requirements 3.2**
  
  - [ ]* 4.7 Write property test for structured output mapping (Property 7)
    - **Property 7: Structured Output Mapping**
    - Generate random responses and DTOs, verify mapping or clear errors
    - **Validates: Requirements 5.1, 5.3, 5.4**
  
  - [ ]* 4.8 Write unit tests for structured and unstructured responses (Example 3)
    - Test requesting structured output (mapped to DTO)
    - Test requesting unstructured output (plain string)
    - **Validates: Requirements 5.5**

- [ ] 5. Enhance MCP tool integration
  - [x] 5.1 Create McpToolConfiguration class
    - Create `@Configuration` class with `@ConditionalOnProperty` for MCP enabled
    - Inject `McpClient` and timeout configuration
    - _Requirements: 4.1, 4.2, 10.4_
  
  - [x] 5.2 Implement ToolCallbackProvider bean
    - Get tools from McpClient and map to ToolCallback instances
    - Implement tool execution with timeout handling
    - Add error handling for tool execution failures
    - Log all available tools at startup with names and descriptions
    - _Requirements: 4.1, 4.3, 4.4, 4.5_
  
  - [ ]* 5.3 Write unit test for MCP tool logging at startup (Example 2)
    - Verify startup logs contain tool names and descriptions
    - **Validates: Requirements 4.3**
  
  - [ ]* 5.4 Write property test for MCP tool execution (Property 5)
    - **Property 5: MCP Tool Execution**
    - Generate random valid tool calls and verify execution and results
    - **Validates: Requirements 4.4**
  
  - [ ]* 5.5 Write property test for tool execution error handling (Property 6)
    - **Property 6: Tool Execution Error Handling**
    - Simulate tool failures and verify error messages and logging
    - **Validates: Requirements 4.5, 7.1, 7.5**
  
  - [x] 5.6 Enable MCP client in application.properties
    - Set `spring.ai.mcp.client.enabled=true`
    - Configure `spring.ai.mcp.client.config-file=classpath:mcp-servers-config.json`
    - Add `spring.ai.mcp.tools.timeout=30s`
    - _Requirements: 4.2, 10.4_

- [x] 6. Checkpoint - Verify MCP integration
  - Ensure MCP tools are loaded and logged at startup
  - Test tool execution with mock tools
  - Verify error handling for tool failures
  - Ask the user if questions arise

- [ ] 7. Update ChatController to use new ChatService
  - [x] 7.1 Refactor ChatController to inject ChatService interface
    - Change injection from `GroqService` to `ChatService`
    - Update error handling to use structured error responses
    - Ensure reactive error handling with `.onErrorResume()`
    - _Requirements: 7.1, 8.3, 9.1_
  
  - [ ]* 7.2 Write property test for reactive error handling (Property 11)
    - **Property 11: Reactive Error Handling**
    - Simulate errors and verify reactive error responses without blocking
    - **Validates: Requirements 8.3, 9.4**
  
  - [ ]* 7.3 Write property test for API backward compatibility (Property 12)
    - **Property 12: API Backward Compatibility**
    - Generate random valid ChatRequest DTOs and verify response structure
    - **Validates: Requirements 9.2, 9.3**
  
  - [ ]* 7.4 Write unit test for API endpoint signature (Example 7)
    - Verify POST /api/chat accepts ChatRequest and returns expected structure
    - **Validates: Requirements 9.1**

- [ ] 8. Implement additional properties and features
  - [x] 8.1 Add dynamic system prompt reload endpoint
    - Create admin endpoint to trigger `SystemPromptProvider.reload()`
    - Add security considerations (restrict to admin users)
    - _Requirements: 6.3_
  
  - [ ]* 8.2 Write property test for dynamic system prompt updates (Property 9)
    - **Property 9: Dynamic System Prompt Updates**
    - Update prompt and verify subsequent requests use new prompt
    - **Validates: Requirements 6.3**
  
  - [x] 8.3 Implement multi-language support validation
    - Ensure system prompt includes language-matching instructions
    - Verify prompt content during initialization
    - _Requirements: 6.4, 6.5_
  
  - [ ]* 8.4 Write property test for multi-language response matching (Property 8)
    - **Property 8: Multi-Language Response Matching**
    - Generate messages in different languages and verify responses match
    - **Validates: Requirements 6.4**
  
  - [x] 8.5 Add streaming response support
    - Implement method returning `Flux<String>` for streaming
    - Use ChatClient's streaming API
    - _Requirements: 8.4_
  
  - [ ]* 8.6 Write unit test for streaming response (Example 6)
    - Verify Flux-based streaming returns response chunks progressively
    - **Validates: Requirements 8.4**

- [ ] 9. Add observability and health checks
  - [x] 9.1 Configure Spring Boot Actuator endpoints
    - Add actuator dependency if not present
    - Expose health, metrics, and info endpoints
    - Configure endpoint security
    - _Requirements: 7.2, 10.1_
  
  - [ ]* 9.2 Write unit test for health check endpoint (Example 5)
    - Verify /actuator/health returns UP status when system is healthy
    - **Validates: Requirements 7.2**
  
  - [x] 9.3 Add custom health indicator for ChatClient
    - Implement `HealthIndicator` that checks ChatClient availability
    - Test with simple ping request to model
    - _Requirements: 7.2_

- [x] 10. Checkpoint - Verify observability and health
  - Test actuator endpoints are accessible
  - Verify health checks report correct status
  - Check metrics are being collected
  - Ask the user if questions arise

- [ ] 11. Configuration and environment management
  - [x] 11.1 Add configuration validation at startup
    - Create `@ConfigurationProperties` class for Spring AI settings
    - Add `@Validated` with constraints (API key not blank, model not blank)
    - Implement fail-fast behavior for invalid configuration
    - _Requirements: 10.5_
  
  - [ ]* 11.2 Write property test for configuration validation (Property 14)
    - **Property 14: Configuration Validation**
    - Test invalid configurations cause startup failure with clear errors
    - **Validates: Requirements 10.5**
  
  - [ ]* 11.3 Write property test for configuration override (Property 13)
    - **Property 13: Configuration Override**
    - Set environment variables and verify they override defaults
    - **Validates: Requirements 10.2**
  
  - [ ]* 11.4 Write unit tests for Spring profile configuration (Example 8)
    - Test dev profile loads development settings
    - Test prod profile loads production settings
    - **Validates: Requirements 10.3**

- [ ] 12. Integration testing and cleanup
  - [ ]* 12.1 Write end-to-end integration tests
    - Test complete chat flow from controller to model
    - Test conversation memory across multiple requests
    - Test MCP tool integration in full flow
    - Test error scenarios end-to-end
    - _Requirements: 11.4, 11.5_
  
  - [x] 12.2 Deprecate and remove old GroqService
    - Mark `GroqService` as `@Deprecated`
    - Remove references to old service
    - Update any remaining code to use `ChatService` interface
    - _Requirements: 12.5_
  
  - [x] 12.3 Add comprehensive JavaDoc documentation
    - Document all public methods and classes
    - Add package-info.java files with package descriptions
    - Include code examples in JavaDoc
    - _Requirements: 12.1, 12.3_
  
  - [x] 12.4 Create migration README documentation
    - Document Spring AI configuration options
    - Explain advisor usage and customization
    - Provide examples of structured outputs
    - Document MCP tool integration
    - Include troubleshooting section
    - _Requirements: 12.2_

- [x] 13. Final checkpoint - Complete validation
  - Run full test suite (unit + property + integration tests)
  - Verify all 14 properties are tested
  - Verify all 8 examples are tested
  - Check test coverage meets >80% goal
  - Verify backward compatibility with existing API
  - Test with real Groq API calls
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional test-related sub-tasks that can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties across random inputs
- Unit tests validate specific examples and edge cases
- Checkpoints ensure incremental validation throughout migration
- The migration maintains backward compatibility until the final cleanup phase
- All Spring AI 1.1.2 best practices are adopted progressively
- MCP tools remain optional and can be disabled via configuration
