# ============================================
# ETAPA 1: Construcción con Maven
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar archivos de Maven primero (mejor cache)
COPY pom.xml .
COPY src ./src

# Compilar el proyecto (saltando tests para evitar fallos)
RUN mvn clean package -DskipTests -q

# ============================================
# ETAPA 2: Imagen ligera para ejecutar
# ============================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el JAR generado desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Puerto que usa Spring Boot
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]