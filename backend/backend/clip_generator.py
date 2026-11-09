
import subprocess
from pathlib import Path


def generate_clip(
    input_path,
    output_path,
    start_seconds,
    end_seconds,
    vertical=True
):
    source = Path(input_path)
    destination = Path(output_path)

    if not source.is_file():
        raise ValueError("El video original no existe.")

    if start_seconds < 0 or end_seconds <= start_seconds:
        raise ValueError("Los tiempos del clip no son válidos.")

    destination.parent.mkdir(parents=True, exist_ok=True)

    command = [
        "ffmpeg",
        "-ss", str(start_seconds),
        "-i", str(source),
        "-t", str(end_seconds - start_seconds),
        "-map", "0:v:0",
        "-map", "0:a?",
    ]

    if vertical:
        command += [
            "-vf",
            "scale=1080:1920:force_original_aspect_ratio=increase,"
            "crop=1080:1920"
        ]

    command += [
        "-c:v", "libx264",
        "-preset", "veryfast",
        "-crf", "23",
        "-c:a", "aac",
        "-b:a", "128k",
        "-movflags", "+faststart",
        "-y",
        str(destination)
    ]

    result = subprocess.run(
        command,
        capture_output=True,
        text=True,
        timeout=1800
    )

    if result.returncode != 0:
        raise RuntimeError(
            "FFmpeg no pudo generar el clip: " + result.stderr[-1500:]
        )

    if not destination.exists() or destination.stat().st_size == 0:
        raise RuntimeError("No se generó el archivo MP4.")

    return str(destination)
