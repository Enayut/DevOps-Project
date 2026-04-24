```bash
docker run -d \
  -p 8081:8080 \
  --name jenkins \
  --add-host=host.docker.internal:host-gateway \
  jenkins/jenkins:lts
```

```bash
curl -X POST http://localhost:8080/api/review \
  -H "Content-Type: application/json" \
  -d '{"repoUrl": "https://github.com/spring-projects/spring-petclinic"}'
```