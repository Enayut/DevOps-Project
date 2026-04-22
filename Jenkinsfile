pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh './mvnw clean package'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                sh '''
                ./mvnw clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                -Dsonar.projectKey=demo \
                -Dsonar.projectName=demo \
                -Dsonar.host.url=http://host.docker.internal:9000 \
                -Dsonar.token=sqp_6c3f1083bac0433bb677cbb89f77cfecf6128e33
                '''
            }
        }
    }
}