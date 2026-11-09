package app.galeria.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.galeria.data.MediaItem
import app.galeria.data.MediaStoreOperations
import app.galeria.data.VideoTrimSpec
import app.galeria.data.VideoTrimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoEditorScreen(item: MediaItem, onClose: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val duration = item.durationMillis ?: 0L
    var start by remember(item.uri) { mutableStateOf(0L) }
    var end by remember(item.uri) { mutableStateOf(duration) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = !saving, onBack = onClose)

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, enabled = !saving) { Icon(GalleryIcons.Close, "Cerrar editor de vídeo") }
            Text("Recortar vídeo", fontSize = 22.sp, modifier = Modifier.weight(1f))
            if (saving) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }
        AndroidView(
            factory = { viewContext ->
                VideoView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    setVideoURI(Uri.parse(item.uri))
                    val controller = MediaController(viewContext)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            onRelease = { it.stopPlayback() },
        )
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            if (duration <= 0L) {
                Text("No se pudo determinar la duración del vídeo", color = Color(0xFFFF8A80))
            } else {
                Text("Inicio ${formatTime(start)}   Fin ${formatTime(end)}   Duración ${formatTime(end - start)}")
                RangeSlider(
                    value = start.toFloat()..end.toFloat(),
                    onValueChange = { range ->
                        start = range.start.toLong().coerceIn(0L, duration)
                        end = range.endInclusive.toLong().coerceIn(0L, duration)
                    },
                    valueRange = 0f..duration.toFloat(),
                    enabled = !saving,
                )
                androidx.compose.material3.Button(
                    enabled = end - start >= 1000L && !saving,
                    onClick = {
                        saving = true
                        error = null
                        val output = File(context.cacheDir, "exports/${UUID.randomUUID()}.mp4")
                        val spec = VideoTrimSpec(duration, start, end)
                        runCatching {
                            VideoTrimmer(context).trim(item, spec, output) { result ->
                                scope.launch {
                                    result.fold(
                                        onSuccess = { trimmed ->
                                            runCatching {
                                                withContext(Dispatchers.IO) {
                                                    MediaStoreOperations.importFile(
                                                        context.contentResolver,
                                                        item.copy(mimeType = "video/mp4"),
                                                        trimmed,
                                                        trimmedName(item.displayName),
                                                    )
                                                }
                                            }.onSuccess {
                                                trimmed.delete()
                                                onSaved()
                                            }.onFailure {
                                                trimmed.delete()
                                                error = "No se pudo guardar el recorte: ${it.message}"
                                                saving = false
                                            }
                                        },
                                        onFailure = {
                                            error = "No se pudo recortar: ${it.message}"
                                            saving = false
                                        },
                                    )
                                }
                            }
                        }.onFailure {
                            output.delete()
                            error = "No se pudo iniciar el recorte: ${it.message}"
                            saving = false
                        }
                    },
                ) { Text(if (saving) "Exportando..." else "Guardar recorte como copia") }
            }
            error?.let { Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

private fun trimmedName(name: String): String {
    val dot = name.lastIndexOf('.')
    return if (dot > 0) "${name.substring(0, dot)}-recorte.mp4" else "$name-recorte.mp4"
}

private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
