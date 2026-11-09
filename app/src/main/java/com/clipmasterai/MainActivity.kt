
package com.clipmasterai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClipMasterApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipMasterApp() {
    val context = LocalContext.current

    var url by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Automático") }
    var aiMode by remember { mutableStateOf("Combinado") }
    var sourceMode by remember { mutableStateOf("Video grabado") }
    var duration by remember { mutableStateOf("30 segundos") }
    var start by remember { mutableStateOf("00:00:00") }
    var end by remember { mutableStateOf("00:00:30") }
    var captions by remember { mutableStateOf(true) }
    var vertical by remember { mutableStateOf(true) }
    var prepareTikTok by remember { mutableStateOf(true) }
    var prepareFacebook by remember { mutableStateOf(true) }
    var status by remember {
        mutableStateOf("Selecciona un video o pega un enlace para comenzar.")
    }
    var selectedClip by remember { mutableStateOf("Clip destacado 1") }
    var showClipDetails by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedVideoUri = uri
                status = "Video seleccionado correctamente."
            } catch (e: Exception) {
                selectedVideoUri = uri
                status = "Video seleccionado. Acceso disponible durante esta sesión."
            }
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

                Text("Convierte videos y lives en clips verticales.")

                Text(
                    "VIDEO DE TU TELÉFONO",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(
                    onClick = {
                        videoPicker.launch(arrayOf("video/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📁 Seleccionar video del teléfono")
                }

                selectedVideoUri?.let { uri ->
                    Text(
                        "Video seleccionado: ${uri.lastPathSegment ?: "Video"}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    TextButton(
                        onClick = {
                            selectedVideoUri = null
                            status = "Video deseleccionado."
                        }
                    ) {
                        Text("Quitar video")
                    }
                }

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enlace de YouTube o Kick") },
                    placeholder = { Text("Pega aquí el enlace...") },
                    singleLine = true
                )

                Text(
                    "Tipo de fuente",
                    style = MaterialTheme.typography.titleMedium
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "Video grabado",
                        "Live terminado",
                        "Live en tiempo real"
                    ).forEach { option ->
                        Row(
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sourceMode == option,
                                onClick = { sourceMode = option }
                            )
                            Text(option)
                        }
                    }
                }

                if (sourceMode == "Live en tiempo real") {
                    Text(
                        "El modo LIVE necesita acceso autorizado a una señal o fuente compatible. No todas las transmisiones permiten captura en tiempo real.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    "Modo de creación",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == "Automático",
                        onClick = { mode = "Automático" },
                        label = { Text("Automático IA") }
                    )

                    FilterChip(
                        selected = mode == "Manual",
                        onClick = { mode = "Manual" },
                        label = { Text("Manual") }
                    )
                }

                if (mode == "Automático") {
                    Text(
                        "Tipo de análisis IA",
                        style = MaterialTheme.typography.titleMedium
                    )

                    listOf("Viral", "Inteligente", "Combinado").forEach {
                        option ->
                        FilterChip(
                            selected = aiMode == option,
                            onClick = { aiMode = option },
                            label = { Text(option) }
                        )
                    }

                    Text(
                        when (aiMode) {
                            "Viral" ->
                                "Busca reacciones, humor, sorpresas y momentos intensos."
                            "Inteligente" ->
                                "Analiza conversaciones, historias, consejos y contenido útil."
                            else ->
                                "Combina detección viral y análisis inteligente."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    "Duración objetivo",
                    style = MaterialTheme.typography.titleMedium
                )

                var expanded by remember { mutableStateOf(false) }

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
                        label = { Text("Duración") }
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
                    Text(
                        "Fragmento que quieres cortar",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Inicio (HH:MM:SS)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("Fin (HH:MM:SS)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    "Edición y publicación",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(captions, { captions = it })
                    Text("Subtítulos automáticos")
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(vertical, { vertical = it })
                    Text("Formato vertical 9:16")
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(prepareTikTok, { prepareTikTok = it })
                    Text("Preparar para TikTok")
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(prepareFacebook, { prepareFacebook = it })
                    Text("Preparar para Facebook Reels")
                }

                Button(
                    onClick = {
                        val validLink =
                            url.contains("youtube.com") ||
                            url.contains("youtu.be") ||
                            url.contains("kick.com")

                        status = when {
                            url.isBlank() && selectedVideoUri == null ->
                                "Selecciona un video o pega un enlace."

                            url.isNotBlank() &&
                                selectedVideoUri == null &&
                                !validLink ->
                                "El enlace debe ser de YouTube o Kick."

                            else ->
                                "Entrada preparada correctamente. Fuente: ${
                                    if (selectedVideoUri != null)
                                        "Video del teléfono"
                                    else
                                        "Enlace externo"
                                }. Modo: $mode. Duración: $duration. Falta conectar el procesador y el motor IA."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✂ GENERAR CLIPS")
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Estado",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(status)
                        Text(
                            "El procesamiento real se conectará en la siguiente etapa."
                        )
                    }
                }

                Text(
                    "Panel de clips",
                    style = MaterialTheme.typography.titleMedium
                )

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Bandeja de clips",
                                style = MaterialTheme.typography.titleSmall
                            )

                            AssistChip(
                                onClick = {
                                    showClipDetails = !showClipDetails
                                },
                                label = {
                                    Text(
                                        if (showClipDetails) "Ocultar"
                                        else "Ver ejemplo"
                                    )
                                }
                            )
                        }

                        if (showClipDetails) {
                            listOf(
                                "Clip destacado 1",
                                "Reacción emocionante",
                                "Momento divertido"
                            ).forEachIndexed { index, clip ->
                                Row(
                                    verticalAlignment =
                                        androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedClip == clip,
                                        onClick = { selectedClip = clip }
                                    )

                                    Column(Modifier.weight(1f)) {
                                        Text(clip)
                                        Text(
                                            "Ejemplo demostrativo · ${15 + index * 10} s",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            status = "$clip seleccionado. Todavía no es un clip real."
                                        }
                                    ) {
                                        Text("Elegir")
                                    }
                                }
                            }
                        }

                        Text(
                            "Los clips de ejemplo no son videos reales generados.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Text(
                    "Destinos: " + listOfNotNull(
                        if (prepareTikTok) "TikTok" else null,
                        if (prepareFacebook) "Facebook Reels" else null
                    ).ifEmpty { listOf("Ninguno") }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "Compartir",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                url.ifBlank { selectedVideoUri?.toString().orEmpty() }
                            )
                        }

                        context.startActivity(
                            Intent.createChooser(send, "Compartir enlace")
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Compartir enlace")
                }

                Text(
                    "La publicación directa en TikTok y Facebook requiere sus APIs y autorización. Esta versión permite seleccionar videos, pero todavía no los procesa.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
