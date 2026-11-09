
from pathlib import Path
import os
import uuid

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, HttpUrl

app = FastAPI(
    title="ClipMaster AI Backend",
    version="0.1.0"
)

JOBS = {}


class VideoRequest(BaseModel):
    url: HttpUrl
    source: str


@app.get("/")
def home():
    return {
        "app": "ClipMaster AI",
        "status": "online",
        "message": "Backend funcionando"
    }


@app.get("/health")
def health():
    return {"status": "healthy"}


@app.post("/jobs")
def create_job(data: VideoRequest):
    url = str(data.url)
    source = data.source.lower().strip()

    if source not in ["youtube", "kick"]:
        raise HTTPException(
            status_code=400,
            detail="Fuente no compatible"
        )

    if source == "youtube" and "youtube.com" not in url and "youtu.be" not in url:
        raise HTTPException(
            status_code=400,
            detail="Enlace de YouTube no válido"
        )

    if source == "kick" and "kick.com" not in url:
        raise HTTPException(
            status_code=400,
            detail="Enlace de Kick no válido"
        )

    job_id = str(uuid.uuid4())

    JOBS[job_id] = {
        "id": job_id,
        "source": source,
        "url": url,
        "status": "pending",
        "message": "Enlace recibido. Pendiente de acceso autorizado al video.",
        "clips": []
    }

    return JOBS[job_id]


@app.get("/jobs/{job_id}")
def get_job(job_id: str):
    if job_id not in JOBS:
        raise HTTPException(
            status_code=404,
            detail="Trabajo no encontrado"
        )

    return JOBS[job_id]
