package com.example.demo.client;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class GitClient {

    public File cloneRepository(String repoUrl) throws IOException, InterruptedException {
        File tempDir = java.nio.file.Files.createTempDirectory("code-review-").toFile();
        
        ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "clone", "--depth", "1", repoUrl, tempDir.getAbsolutePath()
        );
        processBuilder.inheritIO();
        
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("Git clone failed with exit code " + exitCode);
        }
        
        return tempDir;
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
