FROM openjdk:8-jdk-alpine
ADD target/spring-boot-web-0.0.2-SNAPSHOT.jar /app/demologgingapp.jar
VOLUME /tmp
EXPOSE 8080
RUN mkdir -p /app/
RUN mkdir -p /app/logs/
ENTRYPOINT ["java", "-jar", "/app/demologgingapp.jar"]
