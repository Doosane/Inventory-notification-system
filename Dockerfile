FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY build/libs/notification-0.0.1-SNAPSHOT.jar /app/notification-0.0.1-SNAPSHOT.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/notification-0.0.1-SNAPSHOT.jar"]
