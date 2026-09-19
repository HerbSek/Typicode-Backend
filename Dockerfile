# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM payara/micro:6.2024.10-jdk21
ADD --chmod=644 https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar /opt/payara/postgresql.jar
COPY post-boot.asadmin /opt/payara/post-boot.asadmin
COPY --from=build /app/target/*.war /opt/payara/deployments/ROOT.war
EXPOSE 8080
CMD ["--deploymentDir", "/opt/payara/deployments", "--postbootcommandfile", "/opt/payara/post-boot.asadmin", "--addLibs", "/opt/payara/postgresql.jar"]