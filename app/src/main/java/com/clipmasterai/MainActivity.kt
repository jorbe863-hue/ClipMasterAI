package com.clipmasterai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        setContent { ClipMasterApp() }
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
    var status by remember { mutableStateOf("Pega un enlace de YouTube o Kick para comenzar.") }
    var selectedClip by remember { mutableStateOf("Clip destacado 1") }
    var showClipDetails by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("✂ ClipMaster AI", style = MaterialTheme.typography.headlineMedium)
                Text("Convierte videos y lives en clips verticales.")
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Enlace de YouTube o Kick") },
                    placeholder = { Text("Pega aquí el enlace...") },
                    singleLine = true
                )
                Text("Tipo de fuente", style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Video grabado", "Live terminado", "Live en tiempo real").forEach { option ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = sourceMode == option, onClick = { sourceMode = option })
                            Text(option)
                        }
                    }
                }
                if (sourceMode == "Live en tiempo real") {
                    Text(
                        "El modo LIVE necesita acceso autorizado a una señal o fuente compatible. La app podrá marcar momentos destacados y preparar clips; no todas las transmisiones permiten captura en tiempo real.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("Modo de creación", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "Automático", onClick = { mode = "Automático" }, label = { Text("Automático IA") })
                    FilterChip(selected = mode == "Manual", onClick = { mode = "Manual" }, label = { Text("Manual") })
                }
                if (mode == "Automático") {
                    Text("Tipo de análisis IA", style = MaterialTheme.typography.titleMedium)
                    listOf("Viral", "Inteligente", "Combinado").forEach { option ->
                        FilterChip(
                            selected = aiMode == option,
                            onClick = { aiMode = option },
                            label = { Text(option) }
                        )
                    }
                    Text(
                        when (aiMode) {
                            "Viral" -> "Busca reacciones, humor, sorpresas y momentos intensos."
                            "Inteligente" -> "Analiza conversaciones, historias, consejos y contenido útil."
                            else -> "Combina detección viral y análisis inteligente para elegir los mejores momentos."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("Duración objetivo")
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = duration, onValueChange = {}, readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Duración") })
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("15 segundos", "30 segundos", "60 segundos", "90 segundos").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { duration = it; expanded = false })
                        }
                    }
                }
                if (mode == "Manual") {
                    Text("Fragmento que quieres cortar", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(start, { start = it }, label = { Text("Inicio (HH:MM:SS)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(end, { end = it }, label = { Text("Fin (HH:MM:SS)") }, modifier = Modifier.fillMaxWidth())
                }
                Text("Edición y publicación", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = captions, onCheckedChange = { captions = it })
                    Text("Subtítulos automáticos")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = vertical, onCheckedChange = { vertical = it })
                    Text("Formato vertical 9:16")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = prepareTikTok, onCheckedChange = { prepareTikTok = it })
                    Text("Preparar para TikTok")
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = prepareFacebook, onCheckedChange = { prepareFacebook = it })
                    Text("Preparar para Facebook Reels")
                }
                Button(
                    onClick = {
                        status = when {
                            url.isBlank() -> "Primero pega un enlace."
                            !(url.contains("youtube.com") || url.contains("youtu.be") || url.contains("kick.com")) -> "El enlace debe ser de YouTube o Kick."
                            else -> "Solicitud preparada: fuente $sourceMode, modo $mode${if (mode == "Automático") " / análisis $aiMode" else ""}, duración $duration, subtítulos ${if (captions) "sí" else "no"}, vertical ${if (vertical) "sí" else "no"}. Falta conectar el servidor de procesamiento y una fuente autorizada para generar clips reales."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("✂ GENERAR CLIPS") }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estado", style = MaterialTheme.typography.titleMedium)
                        Text(status)
                        Text("Próximo paso: conectar el motor de clips IA y el almacenamiento.")
                    }
                }
                Text("Panel de clips detectados", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Aquí aparecerán los momentos detectados por la IA durante el análisis o el LIVE.",
                    style = MaterialTheme.typography.bodySmall
                )
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Bandeja de clips", style = MaterialTheme.typography.titleSmall)
                            AssistChip(onClick = { showClipDetails = !showClipDetails }, label = { Text(if (showClipDetails) "Ocultar" else "Ver ejemplo") })
                        }
                        if (showClipDetails) {
                            listOf("Clip destacado 1", "Reacción emocionante", "Momento divertido").forEachIndexed { index, clip ->
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    RadioButton(selected = selectedClip == clip, onClick = { selectedClip = clip })
                                    Column(Modifier.weight(1f)) {
                                        Text(clip)
                                        Text("Ejemplo de vista previa · ${15 + index * 10} s", style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = { status = "$clip seleccionado. Los clips reales aparecerán al conectar el motor IA." }) { Text("Elegir") }
                                }
                            }
                        }
                        Text("Los elementos de ejemplo son demostrativos; todavía no hay clips reales generados.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Destinos seleccionados: " + listOfNotNull(
                    if (prepareTikTok) "TikTok" else null,
                    if (prepareFacebook) "Facebook Reels" else null
                ).ifEmpty { listOf("Ninguno") }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                Text("Compartir", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(send, "Compartir enlace"))
                }, modifier = Modifier.fillMaxWidth()) { Text("Compartir enlace") }
                Text("La publicación directa en TikTok/Facebook requiere integrar sus APIs, autorización de la cuenta y revisión de permisos. Esta primera versión es una base de interfaz; todavía no descarga ni procesa videos.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
