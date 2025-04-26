# Этап сборки
FROM gradle:8.10.2-jdk17 AS build
COPY . /app
WORKDIR /app
RUN ./gradlew shadowJar --no-daemon

# Этап запуска
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]