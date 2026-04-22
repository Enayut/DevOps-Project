package com.example.demo.controller;

import com.example.demo.model.ReviewRequest;
import com.example.demo.model.ReviewResponse;
import com.example.demo.service.CodeReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
public class CodeReviewController {

    private final CodeReviewService codeReviewService;

    public CodeReviewController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
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
}
