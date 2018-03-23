FROM openjdk:8-jdk-alpine
# Add Maven dependencies (not shaded into the artifact; Docker-cached)
ADD target/lib           /lib
ARG JAR_FILE
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]