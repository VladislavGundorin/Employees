FROM openjdk:17-jdk-slim

WORKDIR /app
COPY target/domain-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]
