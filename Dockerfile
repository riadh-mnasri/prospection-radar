FROM gradle:8.7-jdk17-alpine AS build
WORKDIR /build
COPY backend/build.gradle.kts backend/settings.gradle.kts ./
RUN gradle dependencies --no-daemon -q || true
COPY backend/src ./src
RUN gradle bootJar -x test --no-daemon -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/build/libs/prospection-radar-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
