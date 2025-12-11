# Etapa 1: Construcción
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:17-jdk-alpine
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
# LIMITAMOS LA MEMORIA AQUÍ: Máximo 350MB para el Heap
ENTRYPOINT ["java", "-Xmx350m", "-Xms128m", "-jar", "/app.jar"]