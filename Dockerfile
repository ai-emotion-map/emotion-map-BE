FROM gradle:7.6.2-jdk17 AS builder
WORKDIR /app

ENV GRADLE_USER_HOME=/app/.gradle

RUN mkdir -p ${GRADLE_USER_HOME} && chown -R gradle:gradle ${GRADLE_USER_HOME}

USER gradle

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle --no-daemon build -x test --parallel || true

COPY . .
RUN gradle --no-daemon clean bootJar -x test


FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
