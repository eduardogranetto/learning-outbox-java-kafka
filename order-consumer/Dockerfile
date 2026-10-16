FROM eclipse-temurin:25-jre
WORKDIR /app
COPY order-consumer/build/libs/*.jar /app/app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
