
import re
from faster_whisper import WhisperModel

_model = None

KEYWORDS = {
    "increible", "increíble", "wow", "brutal", "no puede ser",
    "sorpresa", "ganamos", "perdimos", "mira", "escucha",
    "jamas", "jamás", "nunca", "victoria", "epico", "épico",
    "viral", "locura", "laugh", "wow", "amazing", "crazy"
}


def get_model():
    global _model

    if _model is None:
        _model = WhisperModel(
            "tiny",
            device="cpu",
            compute_type="int8"
        )

    return _model


def detect_highlights(audio_path, window_seconds=30):
    """
    Analiza audio y propone fragmentos interesantes.
    Devuelve tiempos y puntuaciones estimadas.
    """

    if not audio_path:
        raise ValueError("Falta la ruta del audio")

    model = get_model()

    segments, info = model.transcribe(
        audio_path,
        beam_size=1,
        vad_filter=True
    )

    segments = list(segments)

    if not segments:
        return {
            "language": info.language,
            "highlights": []
        }

    duration = max(segment.end for segment in segments)
    highlights = []

    start = 0

    while start < duration:
        end = min(start + window_seconds, duration)

        selected = [
            segment for segment in segments
            if segment.start < end and segment.end > start
        ]

        if selected:
            text = " ".join(s.text.strip() for s in selected)
            words = re.findall(r"\b\w+\b", text.lower())

            keyword_count = sum(
                1 for word in words if word in KEYWORDS
            )

            speech_duration = sum(
                max(0, min(s.end, end) - max(s.start, start))
                for s in selected
            )

            speech_ratio = min(
                speech_duration / max(1, end - start), 1
            )

            score = min(
                100,
                int(
                    35 * speech_ratio
                    + min(len(words), 100) * 0.35
                    + keyword_count * 5
                )
            )

            if len(words) >= 5:
                highlights.append({
                    "start": round(start, 2),
                    "end": round(end, 2),
                    "score": score,
                    "transcript": text
                })

        start += window_seconds

    highlights.sort(
        key=lambda item: item["score"],
        reverse=True
    )

    return {
        "language": info.language,
        "highlights": highlights[:10]
    }
