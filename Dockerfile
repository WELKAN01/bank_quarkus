# syntax=docker/dockerfile:1
# Build a reproducible JVM image for the Quarkus banking API.
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /work

RUN apt-get update \
    && apt-get install --yes --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system quarkus \
    && adduser --system --ingroup quarkus quarkus
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/lib/ /work/lib/
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/*.jar /work/
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/app/ /work/app/
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/quarkus/ /work/quarkus/

USER quarkus
EXPOSE 8080
ENV QUARKUS_HTTP_HOST=0.0.0.0
ENTRYPOINT ["java", "-jar", "/work/quarkus-run.jar"]
