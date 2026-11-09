package app.galeria.ui

import android.graphics.BitmapFactory
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.galeria.data.PrivateVault
import app.galeria.data.VaultEntry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PrivateVaultScreen(
    vault: PrivateVault,
    onClose: () -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var entries by remember { mutableStateOf(vault.entries()) }
    var preview by remember { mutableStateOf<VaultEntry?>(null) }
    var deleting by remember { mutableStateOf<VaultEntry?>(null) }
    val scope = rememberCoroutineScope()
    val refresh = { entries = vault.entries() }
    if (preview != null) {
        VaultPreview(
            entry = requireNotNull(preview),
            vault = vault,
            onClose = { preview = null },
            onRestore = {
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { vault.restore(requireNotNull(preview)) } }
                        .onSuccess {
                            preview = null
                            refresh()
                            onChanged()
                            onMessage("Elemento restaurado")
                        }
                        .onFailure { onMessage("No se pudo restaurar") }
                }
            },
        )
        return
    }
    BackHandler(onBack = onClose)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(180.dp),
        contentPadding = PaddingValues(18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(GalleryIcons.Back, "Salir del cofre") }
                DestinationTitle("Privado")
            }
        }
        if (entries.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MessageDestination("El cofre está vacío", "Mueve elementos aquí desde la selección o el visor.")
            }
        } else {
            items(entries, key = { it.fileName }) { entry ->
                Column {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { preview = entry }) {
                        VaultImage(entry, vault)
                        if (entry.item.mimeType.startsWith("video/")) {
                            Icon(
                                GalleryIcons.Video,
                                "Vídeo privado",
                                modifier = Modifier.align(Alignment.Center).size(40.dp),
                            )
                        }
                    }
                    Text(entry.item.displayName, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                    Row {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { vault.restore(entry) } }
                                    .onSuccess {
                                        refresh()
                                        onChanged()
                                        onMessage("Elemento restaurado")
                                    }
                                    .onFailure { onMessage("No se pudo restaurar") }
                            }
                        }) { Text("Restaurar") }
                        TextButton(onClick = { deleting = entry }) { Text("Borrar") }
                    }
                }
            }
        }
    }
    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Borrar definitivamente") },
            text = { Text("Se eliminará la única copia cifrada de ${entry.item.displayName}.") },
            confirmButton = {
                TextButton(onClick = {
                    vault.deletePermanent(entry)
                    refresh()
                    deleting = null
                    onMessage("Elemento privado eliminado")
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun VaultImage(entry: VaultEntry, vault: PrivateVault, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, entry.fileName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = vault.read(entry)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = entry.item.displayName,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } ?: Box(modifier = modifier.fillMaxSize().background(Color(0xFF17181B)))
}

@Composable
private fun VaultPreview(
    entry: VaultEntry,
    vault: PrivateVault,
    onClose: () -> Unit,
    onRestore: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    var videoFile by remember(entry.fileName) { mutableStateOf<File?>(null) }
    androidx.compose.runtime.LaunchedEffect(entry.fileName) {
        if (entry.item.mimeType.startsWith("video/")) {
            videoFile = withContext(Dispatchers.IO) {
                File.createTempFile("vault-preview", ".mp4", context.cacheDir).apply {
                    writeBytes(vault.read(entry))
                }
            }
        }
    }
    DisposableEffect(entry.fileName) {
        onDispose { videoFile?.delete() }
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (entry.item.mimeType.startsWith("video/")) {
            videoFile?.let { file ->
                AndroidView(
                    factory = { viewContext ->
                        VideoView(viewContext).apply {
                            setMediaController(MediaController(viewContext))
                            setVideoPath(file.absolutePath)
                            setOnPreparedListener { it.isLooping = true; start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            VaultImage(entry, vault, Modifier.fillMaxSize())
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0x99000000)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(GalleryIcons.Back, "Cerrar") }
            Text(entry.item.displayName, modifier = Modifier.weight(1f), maxLines = 1)
            TextButton(onClick = onRestore) { Text("Restaurar") }
        }
    }
}
