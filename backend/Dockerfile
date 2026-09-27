# Use Java 17
FROM eclipse-temurin:17-jdk

# Work inside the container
WORKDIR /app

# Copy Maven project
COPY pom.xml .

# Copy source code
COPY src ./src

# Install Maven
RUN apt-get update \
    && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/*

# Build the application
RUN mvn clean package

# Render provides the PORT environment variable
EXPOSE 8080

# Start the Java application
CMD ["sh", "-c", "java -jar target/hardware-management-app-1.0.0.jar"]