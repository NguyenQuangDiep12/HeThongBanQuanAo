FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src

RUN mvn -B verify

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build --chown=10001:10001 /build/target/shopee-0.0.1-SNAPSHOT.jar ./app.jar

ENV PORT=10000
EXPOSE 10000
USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
