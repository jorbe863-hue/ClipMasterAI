
import re

def detect_highlights(transcript, max_clips=5):
    """
    Detecta fragmentos potencialmente interesantes
    a partir del texto transcrito.
    """
    if not transcript:
        return []

    keywords = [
        "increíble", "no puede ser", "wow",
        "atención", "escucha", "nunca",
        "secreto", "sorprendente", "jajaja",
        "ganó", "perdió", "récord",
        "amazing", "wow", "unbelievable",
        "wait", "crazy", "insane"
    ]

    sentences = re.split(r'(?<=[.!?])\s+', transcript.strip())
    results = []

    for index, sentence in enumerate(sentences):
        score = sum(
            1 for word in keywords
            if word in sentence.lower()
        )

        if score > 0:
            results.append({
                "text": sentence.strip(),
                "score": score
            })

    results.sort(key=lambda item: item["score"], reverse=True)

    return results[:max_clips]
