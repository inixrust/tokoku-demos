# Image demo-web (D9.2, D12.2, D14.2). Base -jdk supaya jcmd tersedia.
# Build jar dulu: .\mvnw.cmd -pl demo-web package -DskipTests
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/demo-web-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
