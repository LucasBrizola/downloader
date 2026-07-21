# downloader

a simple youtube/instagram video downloader

frontend is hosted on netlify, backend is hosted locally, using docker and ngrok to create a connection to use with netlify frontend.
docker runs with yt-dlp and ffmpeg.
videos are first downloaded to a docker container on host pc, then it can be downloaded to the frontend user.

HOW TO RUN:

1) create .jar
2) docker build -t <jar name minus .jar> . (example: "docker build -t downloader .")
3) docker compose up
4) using ngrok : ngrok http 8080


ENDPOINTS:

!-- VIDEOS TO DOWNLOAD
POST http://localhost:8080/api/download
body: 
{
    "urls":
        [
            "video 1",
            "video 2"
        ]
}

RESPONSE:
{
    "queued": [
        "id 1",
        "id 2"
    ],
    "rejected": []
}

!-- CHECK STATUS
GET http://localhost:8080/api/status/{jobid}

RESPONSE:
{
    "status": "",
    "error": ""
}

!-- DOWNLOAD VIDEO
GET http://localhost:8080/api/file/f721534f-190c-49df-aebb-51862b51f6fb
RESPONSE:
video