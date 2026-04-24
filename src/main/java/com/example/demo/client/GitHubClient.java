package com.example.demo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class GitHubClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token}")
    private String githubToken;

    public String createPullRequest(String repoFullName, String branchName, String title, String body) {
        String url = "https://api.github.com/repos/" + repoFullName + "/pulls";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title);
        requestBody.put("body", body);
        requestBody.put("head", branchName);
        requestBody.put("base", "main"); // Assuming 'main' is the default branch

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        headers.set("Accept", "application/vnd.github.v3+json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return (String) response.getBody().get("html_url");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PR on GitHub: " + e.getMessage(), e);
        }
    }
}
