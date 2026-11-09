# ClipMaster AI — Android starter

Primera base Android en Kotlin + Jetpack Compose.

## Abrir
1. Descomprime el proyecto.
2. Abre la carpeta `ClipMasterAI` en Android Studio (versión reciente).
3. Espera la sincronización de Gradle.
4. Ejecuta en un emulador o dispositivo Android.

## Incluido
- Campo para pegar enlaces YouTube/Kick.
- Selector de modo automático/manual.
- Duraciones de 15, 30, 60 y 90 segundos.
- Campos de inicio y fin para recorte manual.
- Compartir enlace mediante el menú de Android.

## Pendiente para una versión funcional completa
- Backend seguro para importar/procesar fuentes permitidas.
- Motor de detección de momentos, transcripción y subtítulos.
- Editor/renderizador vertical 9:16.
- Almacenamiento y descarga de clips.
- Integraciones oficiales de publicación de TikTok y Meta/Facebook, con OAuth y permisos vigentes.

**Nota:** el botón de generar es actualmente una maqueta de flujo; no procesa videos todavía. No se deben extraer videos de terceros sin autorización ni saltarse controles de acceso o restricciones de las plataformas.

## Next-stage preparation
- Added toggles for subtitles, 9:16 output, and TikTok/Facebook Reels targets.
- Added `backend/API_BLUEPRINT.md` and `backend/openapi-outline.yaml` defining the server contract.
- The app remains a frontend starter: real media processing and direct publishing are not active until a secure backend and approved platform integrations are connected.

- Added source selection for recorded videos, ended lives, and real-time live mode.
- Live mode is currently a prepared UI flow, not an active capture implementation; it requires a permitted live feed/source and backend support.

- Added a clip inbox/dashboard preview with selectable sample highlight rows. Sample entries are illustrative only; real clips populate after backend/AI processing is implemented.

## Build APK automatically with GitHub Actions
1. Upload this project to a GitHub repository.
2. Open the repository's Actions tab.
3. Run **Build ClipMaster AI APK** (or push to `main`).
4. Download the `ClipMasterAI-debug-apk` artifact from the completed workflow.
5. Install the APK on your Android device, allowing installation from that source if Android asks.

The workflow creates a debug APK. It is for testing, not a signed production release.
