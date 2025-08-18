FROM gradle:7.6.2-jdk17 AS builder

USER gradle
WORKDIR /home/gradle/app

ENV GRADLE_USER_HOME=/home/gradle/.gradle-docker

COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle ./gradle
RUN gradle --no-daemon build -x test --parallel || true

COPY --chown=gradle:gradle . .
RUN gradle --no-daemon clean bootJar -x test

FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=builder /home/gradle/app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
