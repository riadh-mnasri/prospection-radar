FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B -q
COPY backend/src ./src
RUN mvn clean package -DskipTests -B -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/prospection-radar-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
