# Etapa 1: Construcción (Compilar el Java)
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Correr el Java ligero)
FROM eclipse-temurin:17-jdk-alpine
COPY --from=build /target/*.jar app.jar
# Puerto que usa Render internamente
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]