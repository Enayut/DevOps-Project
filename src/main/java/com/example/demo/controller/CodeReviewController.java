package com.example.demo.controller;

import com.example.demo.model.FixApprovalRequest;
import com.example.demo.model.ReviewRequest;
import com.example.demo.model.ReviewResponse;
import com.example.demo.service.AutoFixService;
import com.example.demo.service.CodeReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class CodeReviewController {

    private final CodeReviewService codeReviewService;
    private final AutoFixService autoFixService;

    public CodeReviewController(CodeReviewService codeReviewService, AutoFixService autoFixService) {
        this.codeReviewService = codeReviewService;
        this.autoFixService = autoFixService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> generateReview(@RequestBody ReviewRequest request) {
        if (request == null || request.getRepoUrl() == null || request.getRepoUrl().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            ReviewResponse response = codeReviewService.processReview(request.getRepoUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/approve-fix")
    public ResponseEntity<Map<String, String>> approveFixAndCreatePR(@RequestBody FixApprovalRequest request) {
        if (request == null || request.getRepoUrl() == null || request.getFilePath() == null || request.getSuggestedFix() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields in FixApprovalRequest"));
        }

        try {
            String prUrl = autoFixService.applyFixAndCreatePR(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "pullRequestUrl", prUrl,
                    "message", "Pull Request successfully created for auto-fix."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

