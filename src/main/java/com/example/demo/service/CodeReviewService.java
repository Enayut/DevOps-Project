package com.example.demo.service;

import com.example.demo.client.GitClient;
import com.example.demo.client.GroqClient;
import com.example.demo.client.SonarQubeClient;
import com.example.demo.model.IssueDetail;
import com.example.demo.model.ReviewResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CodeReviewService {

    private final GitClient gitClient;
    private final SonarQubeClient sonarQubeClient;
    private final GroqClient groqClient;
    
    @Value("${sonar.host.url}")
    private String sonarUrl;

    @Value("${sonar.token}")
    private String sonarToken;

    public CodeReviewService(GitClient gitClient, SonarQubeClient sonarQubeClient, GroqClient groqClient) {
        this.gitClient = gitClient;
        this.sonarQubeClient = sonarQubeClient;
        this.groqClient = groqClient;
    }

    public ReviewResponse processReview(String repoUrl) {
        File repoDir = null;
        try {
            repoDir = gitClient.cloneRepository(repoUrl);
            String projectKey = "temp";

            if (new File(repoDir, "pom.xml").exists()) {
                runJavaAnalysis(repoDir, projectKey);
            } else if (new File(repoDir, "package.json").exists()) {
                System.out.println("Node.js project detected. Sonar automation not configured for JS in MVP.");
                return new ReviewResponse(new ArrayList<>());
            } else if (new File(repoDir, "requirements.txt").exists()) {
                System.out.println("Python project detected. Sonar automation not configured for Python in MVP.");
                return new ReviewResponse(new ArrayList<>());
            } else {
                throw new RuntimeException("Unknown project type");
            }

            List<Map<String, Object>> sonarIssues = sonarQubeClient.fetchIssues(projectKey);
            List<IssueDetail> finalIssues = processIssues(repoDir, sonarIssues);
            
            return new ReviewResponse(finalIssues);

        } catch (Exception e) {
            throw new RuntimeException("Code review process failed: " + e.getMessage(), e);
        } finally {
            gitClient.cleanUp(repoDir);
        }
    }

    private void runJavaAnalysis(File repoDir, String projectKey) throws IOException, InterruptedException {
        // Decide mvn command based on OS or available wrapper
        String mvnCmd = new File(repoDir, "mvnw").exists() ? "./mvnw" : "mvn";
        
        ProcessBuilder processBuilder = new ProcessBuilder(
            mvnCmd, 
            "clean",
            "verify",
            "sonar:sonar",
            "-Dsonar.projectKey=" + projectKey,
            "-Dsonar.host.url=" + sonarUrl,
            "-Dsonar.login=" + sonarToken
        );
        processBuilder.directory(repoDir);
        processBuilder.inheritIO();
        
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("SonarQube analysis failed with exit code " + exitCode);
        }
    }

    private List<IssueDetail> processIssues(File repoDir, List<Map<String, Object>> sonarIssues) {
        List<IssueDetail> issues = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        // Batch processing to avoid rate limits (MVP: top 5 issues)
        int limit = Math.min(5, sonarIssues.size());
        List<Map<String, Object>> batch = sonarIssues.subList(0, limit);

        if (batch.isEmpty()) {
            return issues;
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are a senior developer. Provide a JSON response containing an array of objects. ");
        promptBuilder.append("Each object MUST have these keys: 'id' (integer), 'explanation' (string), and 'suggestedFix' (string replacement for the snippet).\n\n");
        promptBuilder.append("Fix the following ").append(limit).append(" SonarQube issues:\n\n");

        for (int i = 0; i < batch.size(); i++) {
            Map<String, Object> sonarIssue = batch.get(i);
            String rule = (String) sonarIssue.get("rule");
            String message = (String) sonarIssue.get("message");
            String component = (String) sonarIssue.get("component"); 
            String filePath = component.contains(":") ? component.substring(component.indexOf(":") + 1) : component;
            
            Map<String, Object> textRange = (Map<String, Object>) sonarIssue.get("textRange");
            int line = textRange != null && textRange.containsKey("startLine") ? 
                       (Integer) textRange.get("startLine") : 0;

            String codeSnippet = extractCodeSnippet(repoDir, filePath, line);

            promptBuilder.append(String.format("Issue ID: %d\\nRule: %s\\nMessage: %s\\nFile: %s\\nLine: %d\\nCode Snippet:\\n%s\\n\\n",
                    i, rule, message, filePath, line, codeSnippet));

            IssueDetail detail = new IssueDetail();
            detail.setFile(filePath);
            detail.setLine(line);
            detail.setProblem(message);
            issues.add(detail);
        }

        try {
            String aiResponse = groqClient.generate(promptBuilder.toString());
            
            // Clean markdown json block if present
            aiResponse = aiResponse.trim();
            if (aiResponse.startsWith("```json")) {
                aiResponse = aiResponse.substring(7);
            }
            if (aiResponse.endsWith("```")) {
                aiResponse = aiResponse.substring(0, aiResponse.length() - 3);
            }

            // Parse response as a list of maps
            List<Map<String, Object>> parsedList = mapper.readValue(aiResponse, List.class);

            for (Map<String, Object> result : parsedList) {
                int id = (Integer) result.get("id");
                if (id >= 0 && id < issues.size()) {
                    IssueDetail detail = issues.get(id);
                    detail.setExplanation((String) result.get("explanation"));
                    detail.setSuggestedFix((String) result.get("suggestedFix"));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process AI response: " + e.getMessage());
            for (IssueDetail detail : issues) {
                detail.setExplanation("AI Batch Analysis failed or rate limit hit. Error: " + e.getMessage());
                detail.setSuggestedFix("N/A");
            }
        }

        return issues;
    }

    private String extractCodeSnippet(File repoDir, String filePath, int targetLine) {
        try {
            Path path = new File(repoDir, filePath).toPath();
            List<String> lines = Files.readAllLines(path);
            
            if (targetLine == 0 || lines.isEmpty()) return "";
            
            int start = Math.max(0, targetLine - 5);
            int end = Math.min(lines.size(), targetLine + 5);
            
            return String.join("\n", lines.subList(start, end));
        } catch (Exception e) {
            return "// Could not read file content";
        }
    }
}
