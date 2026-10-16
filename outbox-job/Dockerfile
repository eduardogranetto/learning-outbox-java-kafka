FROM eclipse-temurin:25-jre
WORKDIR /app
COPY outbox-job/build/libs/*.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
