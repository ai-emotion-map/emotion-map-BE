FROM gradle:7.6.2-jdk17 AS builder

USER gradle
WORKDIR /home/gradle/app

ENV GRADLE_USER_HOME=/tmp/gradle-cache

COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle ./gradle

RUN gradle --no-daemon build -x test --parallel || true

COPY --chown=gradle:gradle . .

RUN rm -rf /tmp/gradle-cache/* && \
    gradle --no-daemon clean bootJar -x test --refresh-dependencies

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /home/gradle/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
