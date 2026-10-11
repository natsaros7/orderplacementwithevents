# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /builder

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

RUN cp target/*.jar application.jar && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /application

RUN useradd --system --no-create-home --shell /usr/sbin/nologin appuser

COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

USER appuser

EXPOSE 8080

# No HEALTHCHECK: the eclipse-temurin JRE base image ships neither curl nor wget: an orchestrator
# probe against GET /actuator/health/liveness and /actuator/health/readiness
# (management.endpoint.health.probes.enabled=true) is the right call, so this is left to the
# deployment platform (e.g. Kubernetes/OpenShift liveness/readiness probes).

ENTRYPOINT ["java", "-jar", "application.jar"]
