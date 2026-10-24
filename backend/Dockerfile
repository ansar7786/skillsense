# Stage 1: Build JAR with Maven
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime Image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S skillsense && adduser -S skillsense -G skillsense

COPY --from=builder /build/target/*.jar app.jar

USER skillsense

EXPOSE 9000

ENV PORT=9000 \
    SPRING_PROFILES_ACTIVE=prod

CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
