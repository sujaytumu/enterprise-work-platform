FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY api-gateway ./api-gateway
COPY core-processing-engine ./core-processing-engine
COPY payment-switch ./payment-switch
COPY fraud-risk-engine ./fraud-risk-engine
RUN mvn -f api-gateway/pom.xml -DskipTests package && \
    mvn -f core-processing-engine/pom.xml -DskipTests package && \
    mvn -f payment-switch/pom.xml -DskipTests package && \
    mvn -f fraud-risk-engine/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/api-gateway/target/api-gateway-0.1.0.jar ./api-gateway.jar
COPY --from=build /src/core-processing-engine/target/core-processing-engine-0.1.0.jar ./core-processing-engine.jar
COPY --from=build /src/payment-switch/target/payment-switch-0.1.0.jar ./payment-switch.jar
COPY --from=build /src/fraud-risk-engine/target/fraud-risk-engine-0.1.0.jar ./fraud-risk-engine.jar
ENV SERVICE_NAME=payment-switch
CMD ["sh","-c","java -jar /app/$SERVICE_NAME.jar"]