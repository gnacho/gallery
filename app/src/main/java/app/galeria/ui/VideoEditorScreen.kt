package app.galeria.ui

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.galeria.R
import app.galeria.data.MediaItem
import app.galeria.data.MediaStoreOperations
import app.galeria.data.VideoTrimSpec
import app.galeria.data.VideoTrimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@SuppressLint("LocalContextGetResourceValueCall")
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
            IconButton(onClick = onClose, enabled = !saving) { Icon(GalleryIcons.Close, stringResource(R.string.close_video_editor)) }
            Text(stringResource(R.string.trim_video), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
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
                Text(stringResource(R.string.video_duration_unknown), color = MaterialTheme.colorScheme.error)
            } else {
                Text(stringResource(R.string.video_trim_range, formatTime(start), formatTime(end), formatTime(end - start)))
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
                                                error = context.getString(R.string.trim_save_error, it.message)
                                                saving = false
                                            }
                                        },
                                        onFailure = {
                                            error = context.getString(R.string.trim_error, it.message)
                                            saving = false
                                        },
                                    )
                                }
                            }
                        }.onFailure {
                            output.delete()
                            error = context.getString(R.string.trim_start_error, it.message)
                            saving = false
                        }
                    },
                ) { Text(stringResource(if (saving) R.string.exporting else R.string.save_trim_copy)) }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
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
