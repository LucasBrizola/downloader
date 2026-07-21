FROM eclipse-temurin:21-jdk-jammy

# Install Python, pip, ffmpeg (needed for merging audio/video)
RUN apt-get update && \
    apt-get install -y python3-pip ffmpeg && \
    pip3 install -U yt-dlp && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]