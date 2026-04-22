# AGENTS.md — Self-Healing Code Quality DevOps Platform (Groq Edition)

---

## 0. Guiding Principles

1. **Agents assist, humans decide.** No auto-merge. All fixes require approval.
2. **Fail loudly.** Every action is logged in `agent_suggestions`.
3. **Context-aware.** Always pass full source code to agents.
4. **Language-aware.** Java, JS, Python supported.
5. **Idempotent triggers.** Avoid duplicate runs using `(projectKey + buildNumber)`.

---

## 1. Agent Registry

| Agent           | Trigger                  | Output            | Approval |
| --------------- | ------------------------ | ----------------- | -------- |
| CodeReviewAgent | After SonarQube scan     | Issue analysis    | No       |
| AutoFixAgent    | User click / known issue | Code patch (diff) | Yes      |
| RollbackAgent   | Pod crash / high failure | Rollback decision | Optional |

---

## 2. LLM Setup (Groq API)

### Model:

* llama-3.3-70b-versatile (or mixtral-8x7b-32768)

### Config:

```yaml
groq:
  base-url: https://api.groq.com/openai/v1/chat/completions
  model: llama-3.3-70b-versatile
  api-key: ${GROQ_API_KEY}
```

---

## 3. Groq API Client

```java
@Service
public class GroqClient {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${groq.api-key}")
    private String apiKey;

    public String generate(String prompt) {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama3-70b-8192");
        body.put("messages", List.of(message));
        body.put("temperature", 0.0); // low temperature for code generation fixes

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        // Parse OpenAI-compatible response wrapper
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        Map<String, Object> responseMessage = (Map<String, Object>) choices.get(0).get("message");
        return (String) responseMessage.get("content");
    }
}
```

---

## 4. Rule-Based Fix Engine (Pre-AI Layer)

Before calling LLM:

| Language | Tool                                      |
| -------- | ----------------------------------------- |
| Java     | Remove unused imports, replace System.out |
| JS       | ESLint --fix, Prettier                    |
| Python   | autopep8                                  |

Execution:

1. Run formatter/linter
2. Re-check issues
3. Remaining → LLM

---

## 5. CodeReviewAgent

### Purpose:

Analyze SonarQube issues + explain root cause

### Flow:

Sonar → Jenkins → Backend → Agent

### Key Change:

```java
String response = groqClient.generate(prompt);
```

---

## 6. AutoFixAgent

### Purpose:

Generate safe patch

### Prompt Rules:

* Fix ONLY given issue
* No extra refactoring
* Return JSON

### Output:

```json
{
  "autoFixable": true,
  "confidence": 0.85,
  "patchedFile": "...",
  "diff": "..."
}
```

---

## 7. RollbackAgent

### Purpose:

Monitor Kubernetes + rollback if needed

### Trigger:

* CrashLoopBackOff
* Restart spikes

### LLM Call:

```java
String decision = groqClient.generate(prompt);
```

---

## 8. Multi-Language Support

| Language | Detection        | Build  |
| -------- | ---------------- | ------ |
| Java     | pom.xml          | mvn    |
| JS       | package.json     | npm    |
| Python   | requirements.txt | pytest |

---

## 9. Self-Healing Flow

```
Git Push
 ↓
Jenkins Pipeline
 ↓
SonarQube
 ↓
Rule Engine (fast fixes)
 ↓
Groq API (complex/intelligent fixes)
 ↓
Diff Generated
 ↓
User Approval
 ↓
Git Commit / PR
```

---

## 10. Kubernetes Fix Execution

Each fix runs as a Job:

```
Backend → K8s Job → Fix → Push
```

---

## 11. Jenkins Integration

```groovy
stage('Self-Healing') {
    steps {
        sh '''
        curl -X POST http://localhost:8080/api/agent/review/demo/1
        '''
    }
}
```

---

## 12. Database Tables

```sql
agent_suggestions
rollback_incidents
projects
builds
```

---

## 13. Environment Variables

```bash
GROQ_API_KEY=YOUR_GROQ_API_KEY
SONAR_URL=http://localhost:9000
JENKINS_URL=http://localhost:8081
POSTGRES_URL=...
```

---

## 14. MVP Scope (IMPORTANT)

Start with:

* Java project only
* One fix: System.out → logger
* One pipeline

Then expand.

---

## 15. Future Improvements

* GitHub PR automation
* Multi-file fixes
* AI explanation panel
* Learning from past fixes

---

## 16. Key Philosophy

* Rule-based first
* AI second
* Human final authority

---

## 17. System Summary

This platform:

* Detects issues (SonarQube)
* Analyzes (CodeReviewAgent)
* Fixes (AutoFixAgent)
* Recovers (RollbackAgent)
* Learns (future scope)

ALL powered by:

* Jenkins
* Docker
* Kubernetes
* Groq (Fast Inference Cloud API)

---
