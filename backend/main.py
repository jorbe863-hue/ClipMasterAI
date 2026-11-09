
from pathlib import Path
import subprocess
import tempfile
import uuid

from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from fastapi.responses import FileResponse
from pydantic import BaseModel, HttpUrl

from viral_detector import detect_highlights
from clip_generator import generate_clip

app = FastAPI(
    title="ClipMaster AI Backend",
    version="0.3.0"
)

JOBS = {}
OUTPUT_DIR = Path("/app/outputs")
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

ALLOWED_EXTENSIONS = {".mp4", ".mov", ".mkv", ".webm"}
MAX_SIZE = 500 * 1024 * 1024


class VideoRequest(BaseModel):
    url: HttpUrl
    source: str


async def save_upload(video):
    extension = Path(video.filename or "").suffix.lower()

    if extension not in ALLOWED_EXTENSIONS:
        raise HTTPException(400, "Formato de video no compatible.")

    path = Path(tempfile.gettempdir()) / f"{uuid.uuid4()}{extension}"
    size = 0

    with open(path, "wb") as output:
        while True:
            chunk = await video.read(1024 * 1024)
            if not chunk:
                break

            size += len(chunk)

            if size > MAX_SIZE:
                path.unlink(missing_ok=True)
                raise HTTPException(413, "Video superior a 500 MB.")

            output.write(chunk)

    if size == 0:
        path.unlink(missing_ok=True)
        raise HTTPException(400, "El archivo está vacío.")

    return path


@app.get("/")
def home():
    return {
        "app": "ClipMaster AI",
        "status": "online",
        "version": "0.3.0"
    }


@app.get("/health")
def health():
    return {"status": "healthy"}


@app.post("/jobs")
def create_job(data: VideoRequest):
    url = str(data.url)
    source = data.source.lower().strip()

    if source not in ["youtube", "kick"]:
        raise HTTPException(400, "Fuente no compatible.")

    if source == "youtube":
        if "youtube.com" not in url and "youtu.be" not in url:
            raise HTTPException(400, "Enlace de YouTube no válido.")

    if source == "kick" and "kick.com" not in url:
        raise HTTPException(400, "Enlace de Kick no válido.")

    job_id = str(uuid.uuid4())

    JOBS[job_id] = {
        "id": job_id,
        "source": source,
        "url": url,
        "status": "pending",
        "message": "Pendiente de acceso autorizado al contenido.",
        "clips": []
    }

    return JOBS[job_id]


@app.get("/jobs/{job_id}")
def get_job(job_id: str):
    if job_id not in JOBS:
        raise HTTPException(404, "Trabajo no encontrado.")

    return JOBS[job_id]


@app.post("/analyze-upload")
async def analyze_upload(video: UploadFile = File(...)):
    video_path = None

    try:
        video_path = await save_upload(video)

        with tempfile.TemporaryDirectory() as temp_dir:
            audio_path = Path(temp_dir) / "audio.wav"

            result = subprocess.run(
                [
                    "ffmpeg",
                    "-i", str(video_path),
                    "-vn", "-ac", "1",
                    "-ar", "16000",
                    "-y", str(audio_path)
                ],
                capture_output=True,
                text=True,
                timeout=1800
            )

            if result.returncode != 0:
                raise HTTPException(422, "No se pudo extraer el audio.")

            analysis = detect_highlights(str(audio_path))

            return {
                "status": "completed",
                "analysis": analysis
            }

    except HTTPException:
        raise
    except Exception as error:
        raise HTTPException(500, str(error))
    finally:
        if video_path:
            video_path.unlink(missing_ok=True)
        await video.close()


@app.post("/clips/generate")
async def create_clip(
    video: UploadFile = File(...),
    start_seconds: float = Form(...),
    end_seconds: float = Form(...),
    vertical: bool = Form(True)
):
    video_path = None

    try:
        if start_seconds < 0 or end_seconds <= start_seconds:
            raise HTTPException(400, "Los tiempos no son válidos.")

        video_path = await save_upload(video)

        clip_id = str(uuid.uuid4())
        output_path = OUTPUT_DIR / f"{clip_id}.mp4"

        generate_clip(
            str(video_path),
            str(output_path),
            start_seconds,
            end_seconds,
            vertical
        )

        return FileResponse(
            path=str(output_path),
            media_type="video/mp4",
            filename=f"ClipMasterAI_{clip_id}.mp4"
        )

    except HTTPException:
        raise
    except Exception as error:
        raise HTTPException(500, str(error))
    finally:
        if video_path:
            video_path.unlink(missing_ok=True)
        await video.close()
