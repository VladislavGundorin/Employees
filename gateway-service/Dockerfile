FROM openjdk:17-jdk-slim

WORKDIR /app
COPY target/gateway-service-1.0-SNAPSHOT.jar app.jar
RUN mkdir -p /app/logs
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
