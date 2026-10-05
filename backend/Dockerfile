FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY pom.xml ./
RUN mvn -q dependency:go-offline
COPY src ./src
EXPOSE 8080
CMD ["mvn", "spring-boot:run"]
