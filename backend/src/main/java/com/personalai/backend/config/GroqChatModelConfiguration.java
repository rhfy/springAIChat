package com.personalai.backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Manual configuration for Groq API using OpenAI client.
 * 
 * This configuration manually creates OpenAiApi and OpenAiChatModel beans
 * to avoid auto-configuration issues with the extra_body parameter.
 * 
 * Based on official Spring AI Groq integration test:
 * https://github.com/spring-projects/spring-ai/blob/main/models/spring-ai-openai/src/test/java/org/springframework/ai/openai/chat/proxy/GroqWithOpenAiChatModelIT.java
 * 
 * Includes a custom interceptor to remove the 'extra_body' parameter that
 * Groq API does not support.
 */
@Configuration
public class GroqChatModelConfiguration {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens:2000}")
    private Integer maxTokens;

    @Bean
    public GroqRequestInterceptor groqRequestInterceptor() {
        return new GroqRequestInterceptor();
    }

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder(GroqRequestInterceptor groqRequestInterceptor) {
        return RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .requestInterceptor(groqRequestInterceptor);
    }

    @Bean
    @Primary
    public OpenAiApi openAiApi(RestClient.Builder restClientBuilder) {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .build();
    }

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .build())
                .build();
    }

    @Bean
    @Primary
    public ChatClient.Builder chatClientBuilder(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel);
    }
}
