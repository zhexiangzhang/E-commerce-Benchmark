# Build the functions code ...
FROM maven:3.6.3-jdk-11 AS builder
COPY pom.xml /usr/src/app/
COPY src /usr/src/app/src
RUN mvn -f /usr/src/app/pom.xml clean package

# ... and run the web server!
FROM openjdk:8
WORKDIR /
COPY --from=builder /usr/src/app/target/E-commerce-StateFun*jar-with-dependencies.jar e-commerce-StateFun.jar
EXPOSE 1108
CMD java -jar e-commerce-StateFun.jar