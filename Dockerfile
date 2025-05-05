FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/cloud-storage-*.jar app.jar
RUN mkdir -p /app/storage
ENTRYPOINT ["java", "-jar", "app.jar"]