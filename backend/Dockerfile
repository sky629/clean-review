FROM eclipse-temurin:21-jdk AS build

ARG SERVICE_NAME
WORKDIR /workspace

COPY . .
RUN ./gradlew ":${SERVICE_NAME}:bootJar" --no-daemon

FROM eclipse-temurin:21-jre

ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}
WORKDIR /app

COPY --from=build /workspace/${SERVICE_NAME}/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
