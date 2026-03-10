# Stage 1: Build with Maven + JDK 25
FROM maven:3-amazoncorretto-25 AS build
WORKDIR /app/myapp
COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests

# Stage 2: Package the application in a runtime image
FROM amazoncorretto:25-alpine
WORKDIR /app/myapp
COPY --from=build /app/myapp/target/ROOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]