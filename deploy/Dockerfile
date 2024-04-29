FROM openjdk:17-alpine

WORKDIR /app

COPY ../target/load-testing-1.0.0-jar-with-dependencies.jar ./load-testing.jar

CMD ["java", "-jar", "load-testing.jar"]
