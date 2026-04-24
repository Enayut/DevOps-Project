package com.example.demo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OllamaClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ollama.api.url:http://localhost:11434/v1/chat/completions}")
    private String apiUrl;

    @Value("${ollama.model:codellama:7b}")
    private String modelName;

    public String generate(String prompt) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("messages", List.of(message));
        body.put("temperature", 0.0);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> responseMessage = (Map<String, Object>) choices.get(0).get("message");
        return (String) responseMessage.get("content");
    }
}
