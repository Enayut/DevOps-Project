package com.example.demo.model;

import java.util.List;

public class ReviewResponse {
    private List<IssueDetail> issues;

    public ReviewResponse() {}

    public ReviewResponse(List<IssueDetail> issues) {
        this.issues = issues;
    }

    public List<IssueDetail> getIssues() {
        return issues;
    }

    public void setIssues(List<IssueDetail> issues) {
        this.issues = issues;
    }
}
