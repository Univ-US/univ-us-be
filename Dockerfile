# =====================================================================
# Dockerfile — 백엔드 런타임 컨테이너 이미지 정의
#
# [무엇] 빌드된 Spring Boot 실행 JAR 을 담아 'java -jar' 로 띄우는 이미지.
# [왜]   K8s 는 코드가 아니라 '이미지'를 실행함 → 앱을 이미지로 포장해야 배포 가능.
# [흐름] deploy.yml 이 gradlew 로 JAR 빌드 → app.jar 복사 →
#        이 Dockerfile 이 app.jar 를 이미지에 넣음 → GHCR push.
# =====================================================================
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY app.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]