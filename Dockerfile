# Use the official maven/Java 17 image to create a build artifact.
FROM maven:3-openjdk-17 as builder
WORKDIR /app
COPY pom.xml .
# .m2 klasörünü konteynere bağla
VOLUME ["/root/.m2"]
COPY src ./src
RUN mvn package -DskipTests

# Use the official Java 17 image for a lean production stage of our multi-stage build.
FROM openjdk:17-jdk-slim
COPY --from=builder /app/target/brokerage-api-*.jar /brokerage-api.jar
EXPOSE 8080
CMD ["java", "-jar", "/brokerage-api.jar"]

