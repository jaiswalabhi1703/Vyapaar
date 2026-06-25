# Multi-stage build shared by all five Spring Boot services. The compose file passes
# --build-arg MODULE=<service>; the build stage compiles just that module (and its deps)
# from the mono-repo, and the slim runtime stage carries only the resulting jar.
#
#   docker build --build-arg MODULE=order-service -t order-service .

FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build
COPY pom.xml .
COPY common-events/ common-events/
COPY gateway/ gateway/
COPY inventory-service/ inventory-service/
COPY order-service/ order-service/
COPY payment-service/ payment-service/
COPY shipping-service/ shipping-service/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl ${MODULE} -am -DskipTests package && \
    cp ${MODULE}/target/${MODULE}-*.jar /build/app.jar

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /build/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
