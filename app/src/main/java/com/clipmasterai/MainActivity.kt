
package com.clipmasterai

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClipMasterApp() }
    }
}

private fun parseTime(value: String): Long? {
    val parts = value.split(":")
    if (parts.size != 3) return null

    val hours = parts[0].toLongOrNull() ?: return null
    val minutes = parts[1].toLongOrNull() ?: return null
    val seconds = parts[2].toLongOrNull() ?: return null

    if (hours < 0 || minutes !in 0..59 || seconds !in 0..59) {
        return null
    }

    return ((hours * 3600 + minutes * 60 + seconds) * 1000)
}

private fun getVideoDuration(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: 0L
    } finally {
        retriever.release()
    }
}

private fun saveVideoToDevice(
    context: Context,
    source: File
): String {
    val name = "ClipMaster_${System.currentTimeMillis()}.mp4"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/ClipMasterAI"
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: throw IOException("No se pudo crear el archivo en la galería.")

        try {
            val output = resolver.openOutputStream(uri)
                ?: throw IOException("No se pudo abrir el archivo de destino.")

            output.use { stream ->
                source.inputStream().use { input ->
                    input.copyTo(stream)
                }
            }

            val completedValues = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, completedValues, null, null)

            return "Guardado en Películas/ClipMasterAI"
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    val directory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
        "ClipMasterAI"
    )

    if (!directory.exists() && !directory.mkdirs()) {
        throw IOException("No se pudo crear la carpeta de videos.")
    }

    val destination = File(directory, name)
    source.copyTo(destination, overwrite = true)

    return destination.absolutePath
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClipMasterApp() {
    val context = LocalContext.current

    var url by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Automático") }
    var sourceMode by remember { mutableStateOf("Video grabado") }
    var duration by remember { mutableStateOf("30 segundos") }
    var start by remember { mutableStateOf("00:00:00") }
    var end by remember { mutableStateOf("00:00:30") }
    var captions by remember { mutableStateOf(true) }
    var vertical by remember { mutableStateOf(true) }
    var prepareTikTok by remember { mutableStateOf(true) }
    var prepareFacebook by remember { mutableStateOf(true) }
    var status by remember {
        mutableStateOf("Selecciona un video para comenzar.")
    }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var processing by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // El selector puede proporcionar acceso temporal.
            }

            selectedVideoUri = uri
            status = "Video seleccionado correctamente."
        }
    }

    fun generateClip() {
        val uri = selectedVideoUri

        if (uri == null) {
            status = "Selecciona primero un video del teléfono."
            return
        }

        if (processing) return

        try {
            val videoDuration = getVideoDuration(context, uri)

            if (videoDuration <= 0L) {
                status = "No se pudo leer la duración del video."
                return
            }

            val startMs: Long
            val endMs: Long

            if (mode == "Manual") {
                startMs = parseTime(start) ?: run {
                    status = "Formato de inicio incorrecto. Usa HH:MM:SS."
                    return
                }

                endMs = parseTime(end) ?: run {
                    status = "Formato de final incorrecto. Usa HH:MM:SS."
                    return
                }
            } else {
                startMs = 0L
                val seconds = duration.substringBefore(" ").toLongOrNull() ?: 30L
                endMs = seconds * 1000L
            }

            if (startMs >= endMs) {
                status = "El inicio debe ser anterior al final."
                return
            }

            if (endMs > videoDuration) {
                status = "El final supera la duración del video."
                return
            }

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()

            val editedItem = EditedMediaItem.Builder(mediaItem).build()

            val outputFile = File(
                context.cacheDir,
                "clipmaster_${System.currentTimeMillis()}.mp4"
            )

            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {

                    override fun onCompleted(
                        composition: androidx.media3.transformer.Composition,
                        exportResult: ExportResult
                    ) {
                        Thread {
                            val result = runCatching {
                                saveVideoToDevice(context, outputFile)
                            }

                            Handler(Looper.getMainLooper()).post {
                                processing = false

                                status = result.fold(
                                    onSuccess = {
                                        outputFile.delete()
                                        "¡Clip creado correctamente! $it"
                                    },
                                    onFailure = {
                                        "El video se procesó, pero no se pudo guardar: ${it.message}"
                                    }
                                )
                            }
                        }.start()
                    }

                    override fun onError(
                        composition: androidx.media3.transformer.Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        processing = false
                        outputFile.delete()
                        status = "Error procesando el video: ${exportException.message}"
                    }
                })
                .build()

            processing = true
            status = "Procesando video. Espera a que termine..."

            transformer.start(editedItem, outputFile.absolutePath)

        } catch (e: Exception) {
            processing = false
            status = "No se pudo iniciar el procesamiento: ${e.message}"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "✂ ClipMaster AI",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text("Convierte videos en clips cortos.")

                OutlinedButton(
                    onClick = {
                        videoPicker.launch(arrayOf("video/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📁 Seleccionar video del teléfono")
                }

                Text(
                    selectedVideoUri?.lastPathSegment
                        ?: "Ningún video seleccionado",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enlace de YouTube o Kick") },
                    singleLine = true
                )

                Text("Tipo de fuente")
                listOf(
                    "Video grabado",
                    "Live terminado",
                    "Live en tiempo real"
                ).forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = sourceMode == option,
                            onClick = { sourceMode = option }
                        )
                        Text(option)
                    }
                }

                Text("Modo de creación")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "Automático",
                        onClick = { mode = "Automático" },
                        label = { Text("Automático") }
                    )
                    FilterChip(
                        selected = mode == "Manual",
                        onClick = { mode = "Manual" },
                        label = { Text("Manual") }
                    )
                }

                if (mode == "Automático") {
                    Text(
                        "Recorte provisional: primeros segundos del video. "
                            + "La detección inteligente llegará después."
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text("Duración del clip") }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf(
                            "15 segundos",
                            "30 segundos",
                            "60 segundos",
                            "90 segundos"
                        ).forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    duration = it
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (mode == "Manual") {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Inicio HH:MM:SS") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("Final HH:MM:SS") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Opciones de edición")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(captions, { captions = it })
                    Text("Subtítulos automáticos (pendiente)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(vertical, { vertical = it })
                    Text("Formato vertical 9:16 (pendiente)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(prepareTikTok, { prepareTikTok = it })
                    Text("Preparar para TikTok")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(prepareFacebook, { prepareFacebook = it })
                    Text("Preparar para Facebook Reels")
                }

                Button(
                    onClick = { generateClip() },
                    enabled = !processing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (processing) "PROCESANDO..."
                        else "✂ GENERAR CLIP"
                    )
                }

                if (processing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Estado del procesamiento",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(status)
                    }
                }

                Text(
                    "Los enlaces de YouTube y Kick todavía no se descargan ni procesan. "
                        + "Selecciona un archivo de video local para probar el recorte."
                )
            }
        }
    }
}
