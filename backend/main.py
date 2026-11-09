
from pathlib import Path
import subprocess
import tempfile
import uuid
import os

from fastapi import FastAPI, HTTPException, UploadFile, File
from pydantic import BaseModel, HttpUrl

from viral_detector import detect_highlights

app = FastAPI(
    title="ClipMaster AI Backend",
    version="0.2.0"
)

JOBS = {}

ALLOWED_EXTENSIONS = {".mp4", ".mov", ".mkv", ".webm"}
MAX_SIZE = 500 * 1024 * 1024


class VideoRequest(BaseModel):
    url: HttpUrl
    source: str


@app.get("/")
def home():
    return {
        "app": "ClipMaster AI",
        "status": "online"
    }


@app.get("/health")
def health():
    return {"status": "healthy"}


@app.post("/jobs")
def create_job(data: VideoRequest):
    url = str(data.url)
    source = data.source.lower().strip()

    if source not in ["youtube", "kick"]:
        raise HTTPException(400, "Fuente no compatible")

    if source == "youtube":
        if "youtube.com" not in url and "youtu.be" not in url:
            raise HTTPException(400, "Enlace de YouTube no válido")

    if source == "kick" and "kick.com" not in url:
        raise HTTPException(400, "Enlace de Kick no válido")

    job_id = str(uuid.uuid4())

    JOBS[job_id] = {
        "id": job_id,
        "source": source,
        "url": url,
        "status": "pending",
        "message": "Enlace recibido. Requiere acceso autorizado al contenido.",
        "clips": []
    }

    return JOBS[job_id]


@app.get("/jobs/{job_id}")
def get_job(job_id: str):
    if job_id not in JOBS:
        raise HTTPException(404, "Trabajo no encontrado")

    return JOBS[job_id]


@app.post("/analyze-upload")
async def analyze_upload(video: UploadFile = File(...)):
    extension = Path(video.filename or "").suffix.lower()

    if extension not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            400,
            "Formato no compatible. Usa MP4, MOV, MKV o WEBM."
        )

    try:
        with tempfile.TemporaryDirectory() as temp_dir:
            video_path = Path(temp_dir) / f"video{extension}"
            audio_path = Path(temp_dir) / "audio.wav"

            size = 0

            with open(video_path, "wb") as output:
                while True:
                    chunk = await video.read(1024 * 1024)

                    if not chunk:
                        break

                    size += len(chunk)

                    if size > MAX_SIZE:
                        raise HTTPException(
                            413,
                            "El video supera el límite de 500 MB."
                        )

                    output.write(chunk)

            if size == 0:
                raise HTTPException(400, "El archivo está vacío.")

            result = subprocess.run(
                [
                    "ffmpeg",
                    "-i", str(video_path),
                    "-vn",
                    "-ac", "1",
                    "-ar", "16000",
                    "-y",
                    str(audio_path)
                ],
                capture_output=True,
                text=True,
                timeout=1800
            )

            if result.returncode != 0:
                raise HTTPException(
                    422,
                    "No se pudo extraer el audio del video."
                )

            analysis = detect_highlights(str(audio_path))

            return {
                "status": "completed",
                "message": "Análisis terminado.",
                "analysis": analysis
            }

    except HTTPException:
        raise

    except subprocess.TimeoutExpired:
        raise HTTPException(504, "El procesamiento tardó demasiado.")

    except Exception as error:
        raise HTTPException(
            500,
            f"Error durante el análisis: {str(error)}"
        )

    finally:
        await video.close()
