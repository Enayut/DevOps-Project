package com.example.demo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class SonarQubeClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sonar.host.url}")
    private String sonarUrl;

    @Value("${sonar.token}")
    private String sonarToken;

    public List<Map<String, Object>> fetchIssues(String projectKey) {
        String url = sonarUrl + "/api/issues/search?projectKeys=" + projectKey;

        HttpHeaders headers = new HttpHeaders();
        String auth = sonarToken + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("issues")) {
                return (List<Map<String, Object>>) body.get("issues");
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch Sonar issues: " + e.getMessage());
        }

        return new ArrayList<>();
    }
}
