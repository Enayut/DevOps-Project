package com.example.demo.service;

import com.example.demo.client.GitClient;
import com.example.demo.client.GitHubClient;
import com.example.demo.model.FixApprovalRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class AutoFixService {

    private final GitClient gitClient;
    private final GitHubClient gitHubClient;

    public AutoFixService(GitClient gitClient, GitHubClient gitHubClient) {
        this.gitClient = gitClient;
        this.gitHubClient = gitHubClient;
    }

    public String applyFixAndCreatePR(FixApprovalRequest request) {
        File repoDir = null;
        try {
            // 1. Clone Repo
            repoDir = gitClient.cloneRepository(request.getRepoUrl());
            
            // 2. Create Branch
            String branchName = "auto-fix-" + System.currentTimeMillis();
            gitClient.checkoutNewBranch(repoDir, branchName);

            // 3. Apply Fix to File
            Path targetFile = new File(repoDir, request.getFilePath()).toPath();
            if (!Files.exists(targetFile)) {
                throw new RuntimeException("Target file not found in repository: " + request.getFilePath());
            }

            List<String> lines = Files.readAllLines(targetFile);
            int idx = request.getLine() - 1; // 0-based array index
            
            if (idx >= 0 && idx < lines.size()) {
                // Simplistic replace for MVP: Replace the whole line with suggested fix
                // A better approach would be proper regex matching or diffs based on LLM output
                lines.set(idx, request.getSuggestedFix());
            } else {
                throw new RuntimeException("Invalid line number: " + request.getLine());
            }

            Files.write(targetFile, lines);

            // 4. Commit and Push
            String commitMsg = "AutoFix: " + request.getIssueMessage();
            gitClient.commitChanges(repoDir, commitMsg);
            gitClient.pushBranch(repoDir, branchName, request.getRepoUrl());

            // 5. Open Pull Request
            String repoFullName = extractRepoFullName(request.getRepoUrl());
            String prBody = "### AutoFix Patch\\n**Fix applied for:** " + request.getIssueMessage();
            String prUrl = gitHubClient.createPullRequest(repoFullName, branchName, commitMsg, prBody);

            return prUrl;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply fix and create PR: " + e.getMessage(), e);
        } finally {
            gitClient.cleanUp(repoDir);
        }
    }

    private String extractRepoFullName(String repoUrl) {
        // Assuming format is https://github.com/user/repo or similar
        String u = repoUrl.replace(".git", "");
        String[] parts = u.split("github.com/");
        if (parts.length > 1) {
            return parts[1]; // user/repo
        }
        return u; // fallback
    }
}
