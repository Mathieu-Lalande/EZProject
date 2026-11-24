# Image unique : API Spring Boot + interface React (servie par le jar).
# docker build -t ezproject .   (ou : docker compose up --build)

# 1) Interface
FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) API (l'interface construite est embarquée dans classpath:/static)
FROM maven:3.9-eclipse-temurin-26 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package

# 3) Exécution : JRE seul, utilisateur non root
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=backend /build/backend/target/ezproject-backend-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
