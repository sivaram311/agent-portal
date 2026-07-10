# Optional container API (Compose profile: container-api).
# Build context must be the parent workspace (MyWorkspace) so the CSS starter is available:
#   docker build -f agent-portal/backend/Dockerfile -t agent-portal-backend ..
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY centralized-security-system/clients/spring-boot-starter /build/starter
RUN mvn -B -DskipTests -f /build/starter/pom.xml install

COPY agent-portal/backend /build/backend
RUN mvn -B -DskipTests -f /build/backend/pom.xml package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p /workspaces
COPY --from=build /build/backend/target/backend-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENV AGENT_WORKSPACE_ROOT=/workspaces \
    SPRING_PROFILES_ACTIVE=postgres \
    SPRING_H2_CONSOLE_ENABLED=false
VOLUME ["/workspaces"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
