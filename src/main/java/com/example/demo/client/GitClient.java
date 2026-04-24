package com.example.demo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class GitClient {

    @Value("${github.token}")
    private String githubToken;

    public File cloneRepository(String repoUrl) throws IOException, InterruptedException {
        File tempDir = java.nio.file.Files.createTempDirectory("code-review-").toFile();
        
        // Use token in URL for clone if not already present
        String authenticatedUrl = embedTokenInUrl(repoUrl);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "clone", "--depth", "1", authenticatedUrl, tempDir.getAbsolutePath()
        );
        processBuilder.inheritIO();
        
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("Git clone failed with exit code " + exitCode);
        }
        
        return tempDir;
    }

    public void checkoutNewBranch(File repoDir, String branchName) throws IOException, InterruptedException {
        runCommand(repoDir, "git", "checkout", "-b", branchName);
    }

    public void commitChanges(File repoDir, String commitMessage) throws IOException, InterruptedException {
        // Configure bare minimum git user logic locally so commits don't fail
        runCommand(repoDir, "git", "config", "user.email", "bot@auto-fix.internal");
        runCommand(repoDir, "git", "config", "user.name", "AutoFixBot");

        runCommand(repoDir, "git", "add", ".");
        runCommand(repoDir, "git", "commit", "-m", commitMessage);
    }

    public void pushBranch(File repoDir, String branchName, String repoUrl) throws IOException, InterruptedException {
        String authenticatedUrl = embedTokenInUrl(repoUrl);
        // Explicitly set the remote url to include the token for pushing
        runCommand(repoDir, "git", "remote", "set-url", "origin", authenticatedUrl);
        runCommand(repoDir, "git", "push", "-u", "origin", branchName);
    }

    private String embedTokenInUrl(String repoUrl) {
        if (githubToken != null && !githubToken.isEmpty() && !repoUrl.contains("@")) {
            return repoUrl.replace("https://", "https://oauth2:" + githubToken + "@");
        }
        return repoUrl;
    }

    private void runCommand(File directory, String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(directory);
        processBuilder.inheritIO();
        
        Process process = processBuilder.start();
        if (process.waitFor() != 0) {
            throw new RuntimeException("Git command failed: " + String.join(" ", command));
        }
    }

    public void cleanUp(File directory) {
        if (directory != null && directory.exists()) {
            deleteDirectory(directory);
        }
    }

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}
