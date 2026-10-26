# Stage 1: Build with Maven
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -q -DskipTests

# Stage 2: Run with lightweight JRE
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=build /app/target/examination-timetabling-problem-2.4.0.jar app.jar
COPY --from=build /app/src/main/resources/web ./src/main/resources/web

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
