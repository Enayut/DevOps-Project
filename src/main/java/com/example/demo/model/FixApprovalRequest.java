package com.example.demo.model;

import lombok.Data;

@Data
public class FixApprovalRequest {
    private String repoUrl;      // e.g., "https://github.com/user/repo"
    private String filePath;     // e.g., "src/main/java/Example.java"
    private String suggestedFix; // the replacement string
    private int line;            // target line to help locate the fix
    private String issueMessage; // e.g., "Remove unused import"
}
