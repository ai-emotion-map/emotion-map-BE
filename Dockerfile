FROM gradle:7.6.2-jdk17 AS builder
WORKDIR /app
USER gradle

COPY build.gradle settings.gradle ./
COPY gradle ./gradle

RUN gradle build -x test --parallel || return 0

COPY . .

RUN gradle clean bootJar -x test

FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
