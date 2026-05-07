# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy root pom.xml
COPY pom.xml .

# Copy module poms
COPY shared-module/pom.xml shared-module/
COPY server-module/pom.xml server-module/
COPY client-module/pom.xml client-module/

# Tải plugin
RUN mvn dependency:resolve-plugins -B

# Copy source code
COPY shared-module/src shared-module/src
COPY server-module/src server-module/src

# Build và cài đặt shared-module trước
RUN mvn install -pl shared-module -am -DskipTests

# Build package cho server-module
RUN mvn package -pl server-module -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file JAR
COPY --from=build /app/server-module/target/server-1.0-SNAPSHOT.jar app.jar

ENV DB_HOST=db
ENV DB_PORT=3306
ENV DB_NAME=daugiadb
ENV DB_USERNAME=root
ENV DB_PASSWORD=root

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
