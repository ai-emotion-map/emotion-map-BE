FROM gradle:7.6.2-jdk17 AS builder
WORKDIR /app

USER gradle

COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle gradle ./gradle

RUN gradle build -x test --parallel || return 0

COPY --chown=gradle:gradle . .
RUN gradle clean bootJar -x test


FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
