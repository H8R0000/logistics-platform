# Build a single selected service and the Maven modules it depends on.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
ARG MODULE
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -pl "${MODULE}" -am -DskipTests package \
    && cp "${MODULE}"/target/*.jar /application.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /application.jar application.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
