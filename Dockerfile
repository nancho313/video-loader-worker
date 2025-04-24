FROM openjdk:21-slim
LABEL org.opencontainers.image.source="https://github.com/nancho313/video-loader-worker"
LABEL org.opencontainers.image.description="video loader worker"
COPY target/video*.jar app.jar
RUN mkdir -p /videos_folder
CMD ["java", "-jar", "/app.jar"]