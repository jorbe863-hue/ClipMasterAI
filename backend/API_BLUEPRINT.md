# ClipMaster AI — backend integration blueprint

This starter does not yet download, analyze, render, or publish videos. This document defines the next integration boundary.

## Proposed API

- `POST /v1/jobs` — create a clip-generation job from an authorized source URL.
  - `source`: `youtube` or `kick`
  - `url`: source URL
  - `mode`: `viral`, `intelligent`, or `combined`
  - `duration_seconds`: 15, 30, 60, or 90
  - `manual_start` / `manual_end`: optional timestamps
  - `captions`: boolean
  - `aspect_ratio`: `9:16`
- `GET /v1/jobs/{job_id}` — job status and progress.
- `GET /v1/jobs/{job_id}/clips` — generated clip metadata and authorized download URLs.
- `POST /v1/publishing/tiktok` — initiate official TikTok publishing after OAuth and permission checks.
- `POST /v1/publishing/facebook` — initiate official Meta/Facebook Reels publishing after OAuth and permission checks.

## Processing pipeline

1. Validate source ownership/permission and platform terms.
2. Ingest only media the user is authorized to process, using official or rights-cleared access.
3. Transcribe audio and detect scene changes / salient moments.
4. Score moments:
   - Viral: reactions, laughter, excitement, surprise, high-energy speech.
   - Intelligent: useful explanations, stories, advice, coherent topic segments.
   - Combined: merge and rank both score sets, remove duplicates.
5. Render 9:16 clips, captions, and safe-area-aware layouts.
6. Store outputs securely with expiring download links.
7. Publish only through official platform APIs and account authorization.

## Credentials and deployment

Keep API secrets on a secure backend, never inside the Android APK. Production publishing requires developer applications, OAuth consent, platform review/permissions, and compliance with current TikTok and Meta requirements. Do not scrape private streams, bypass access controls, or download content without rights.

## Live mode extension
- Add a live-session lifecycle (`POST /v1/live-sessions`, `GET /v1/live-sessions/{id}`, `DELETE /v1/live-sessions/{id}`).
- Accept only supported, authorized live inputs or creator-provided streams/feeds.
- Maintain a rolling buffer only where licensing, platform terms, and user consent allow it.
- Emit timestamped highlight events to the Android app; render clips only from media the user is authorized to use.
- Platform URLs alone do not guarantee access to a live media stream. Some platforms restrict capture or provide no public live-ingest endpoint.
