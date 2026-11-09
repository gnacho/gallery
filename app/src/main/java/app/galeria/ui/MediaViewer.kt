package app.galeria.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.print.PrintManager
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.print.PrintHelper
import app.galeria.R
import app.galeria.data.MediaDetails
import app.galeria.data.MediaDetailsReader
import app.galeria.data.MediaItem
import app.galeria.data.PlaybackSpeed
import app.galeria.data.TrashRetention
import app.galeria.data.TrashRetentionReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun ViewerAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label)
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaViewer(
    items: List<MediaItem>,
    initialItem: MediaItem,
    onClose: () -> Unit,
    onShare: (List<MediaItem>) -> Unit,
    onFavorite: (List<MediaItem>) -> Unit,
    onTrash: (List<MediaItem>) -> Unit,
    onDelete: (List<MediaItem>) -> Unit,
    onEdit: (MediaItem) -> Unit,
    onEditVideo: (MediaItem) -> Unit,
    onDuplicate: (MediaItem) -> Unit,
    onRename: (MediaItem, String) -> Unit,
    onPrivate: (List<MediaItem>) -> Unit,
    onMessage: (String) -> Unit,
    slideshowSeconds: Int = 4,
    notes: Map<String, String> = emptyMap(),
    onSaveNote: (MediaItem, String) -> Unit = { _, _ -> },
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val initialPage = items.indexOfFirst { it.uri == initialItem.uri }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = initialPage, pageCount = { items.size })
    var detailsVisible by remember { mutableStateOf(false) }
    var moreVisible by remember { mutableStateOf(false) }
    var renameVisible by remember { mutableStateOf(false) }
    var noteVisible by remember { mutableStateOf(false) }
    var noteValue by remember { mutableStateOf("") }
    var renameValue by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var comparing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val item = items.getOrElse(pager.currentPage) { initialItem }
    val date = remember(item) {
        item.dateTakenMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES")))
        }
    }
    LaunchedEffect(playing, pager.currentPage) {
        if (playing && items.isNotEmpty()) {
            delay(slideshowSeconds * 1000L)
            pager.animateScrollToPage((pager.currentPage + 1) % items.size)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (comparing) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { ViewerPage(item) }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ViewerPage(items[(pager.currentPage + 1) % items.size])
                }
            }
        } else {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                ViewerPage(items[page])
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .background(Color(0x99000000)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(GalleryIcons.Back, stringResource(R.string.back)) }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.displayName, color = Color.White, maxLines = 1, fontSize = 15.sp)
                date?.let { Text(it, fontSize = 12.sp, color = Color(0xFFC8CAD0)) }
            }
            if (comparing) {
                IconButton(onClick = { comparing = false }) { Icon(GalleryIcons.Compare, stringResource(R.string.end_compare)) }
            } else {
                TextButton(
                    onClick = { detailsVisible = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) { Text(stringResource(R.string.details)) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .background(Color(0x99000000)).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewerAction(GalleryIcons.Share, stringResource(R.string.share)) { onShare(listOf(item)) }
            ViewerAction(GalleryIcons.Favorite, stringResource(R.string.favorite)) { onFavorite(listOf(item)) }
            if (item.mimeType.startsWith("image/")) {
                ViewerAction(GalleryIcons.Edit, stringResource(R.string.edit)) { onEdit(item) }
            }
            if (item.mimeType.startsWith("video/")) {
                ViewerAction(GalleryIcons.Edit, stringResource(R.string.trim)) { onEditVideo(item) }
            }
            ViewerAction(GalleryIcons.Delete, if (item.isTrashed) stringResource(R.string.restore) else stringResource(R.string.delete)) {
                onTrash(listOf(item))
            }
            Box {
                ViewerAction(GalleryIcons.More, stringResource(R.string.more)) { moreVisible = true }
                DropdownMenu(expanded = moreVisible, onDismissRequest = { moreVisible = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (playing) R.string.stop_slideshow else R.string.slideshow)) },
                        leadingIcon = { Icon(GalleryIcons.Slideshow, null) },
                        onClick = {
                            playing = !playing
                            moreVisible = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.compare_next)) },
                        leadingIcon = { Icon(GalleryIcons.Compare, null) },
                        onClick = {
                            comparing = true
                            playing = false
                            moreVisible = false
                        },
                    )
                    if (item.mimeType.startsWith("image/")) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.set_wallpaper)) },
                            leadingIcon = { Icon(GalleryIcons.Wallpaper, null) },
                            onClick = {
                                moreVisible = false
                                runCatching {
                                    val uri = Uri.parse(item.uri)
                                    val intent = android.app.WallpaperManager.getInstance(context)
                                        .getCropAndSetWallpaperIntent(uri)
                                    context.startActivity(intent)
                                }.onFailure { onMessage(context.getString(R.string.wallpaper_failed)) }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.print)) },
                            leadingIcon = { Icon(GalleryIcons.Print, null) },
                            onClick = {
                                moreVisible = false
                                runCatching {
                                    PrintHelper(context).apply {
                                        scaleMode = PrintHelper.SCALE_MODE_FIT
                                        printBitmap(item.displayName, Uri.parse(item.uri))
                                    }
                                }.onFailure { onMessage(context.getString(R.string.print_failed)) }
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_with)) },
                        onClick = {
                            moreVisible = false
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(item.uri), item.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with))) }
                                .onFailure { onMessage(context.getString(R.string.no_compatible_app)) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.duplicate)) },
                        onClick = {
                            moreVisible = false
                            onDuplicate(item)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = {
                            moreVisible = false
                            renameValue = item.displayName
                            renameVisible = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_note)) },
                        onClick = {
                            moreVisible = false
                            noteValue = notes[item.uri].orEmpty()
                            noteVisible = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.move_to_private)) },
                        leadingIcon = { Icon(GalleryIcons.Private, null) },
                        onClick = {
                            moreVisible = false
                            onPrivate(listOf(item))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_permanently)) },
                        onClick = {
                            moreVisible = false
                            onDelete(listOf(item))
                        },
                    )
                }
            }
        }
    }
    if (detailsVisible) {
        ModalBottomSheet(onDismissRequest = { detailsVisible = false }) {
            MediaDetailsPanel(item)
        }
    }
    if (noteVisible) {
        AlertDialog(
            onDismissRequest = { noteVisible = false },
            title = { Text(stringResource(R.string.note_title)) },
            text = {
                OutlinedTextField(
                    value = noteValue,
                    onValueChange = { noteValue = it },
                    label = { Text(stringResource(R.string.searchable_text)) },
                    minLines = 2,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    noteVisible = false
                    onSaveNote(item, noteValue)
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { noteVisible = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (renameVisible) {
        AlertDialog(
            onDismissRequest = { renameVisible = false },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameValue.isNotBlank(),
                    onClick = {
                        renameVisible = false
                        onRename(item, renameValue.trim())
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { renameVisible = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ViewerPage(item: MediaItem) {
    var transform by remember(item.uri) { mutableStateOf(ViewerTransform()) }
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(item.uri) {
            detectTransformGestures { _, pan, zoom, _ ->
                transform = transform.zoomBy(zoom).panBy(pan.x, pan.y)
            }
        }.graphicsLayer {
            scaleX = transform.scale
            scaleY = transform.scale
            translationX = transform.offsetX
            translationY = transform.offsetY
        },
        contentAlignment = Alignment.Center,
    ) {
        when {
            item.mimeType.startsWith("video/") -> VideoPlayer(item)
            item.mimeType == "image/gif" || item.mimeType == "image/webp" -> AnimatedImage(item)
            else -> MediaImage(item, modifier = Modifier.fillMaxSize())
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(item: MediaItem) {
    val context = LocalContext.current
    var speed by remember(item.uri) { mutableStateOf(1f) }
    val player = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(Uri.parse(item.uri)))
            prepare()
        }
    }
    LaunchedEffect(player, speed) { player.setPlaybackSpeed(speed) }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext -> PlayerView(viewContext).apply { this.player = player } },
            update = { it.player = player },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        TextButton(
            onClick = { speed = PlaybackSpeed.next(speed) },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) { Text(stringResource(R.string.speed_x, speed)) }
    }
}

@Composable
private fun AnimatedImage(item: MediaItem) {
    val context = LocalContext.current
    val drawable by produceState<Drawable?>(initialValue = null, item.uri) {
        value = runCatching {
            withContext(Dispatchers.IO) {
                val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(item.uri))
                ImageDecoder.decodeDrawable(source)
            }
        }.getOrNull()
    }
    DisposableEffect(drawable) {
        (drawable as? AnimatedImageDrawable)?.start()
        onDispose { (drawable as? AnimatedImageDrawable)?.stop() }
    }
    if (drawable != null) {
        AndroidView(
            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { it.setImageDrawable(drawable) },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        MediaImage(item, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun MediaDetailsPanel(item: MediaItem) {
    val context = LocalContext.current
    val details by produceState<MediaDetails?>(initialValue = null, item.uri) {
        value = runCatching {
            withContext(Dispatchers.IO) { MediaDetailsReader.read(context, item) }
        }.getOrNull()
    }
    val retention by produceState<TrashRetention?>(initialValue = null, item.uri, item.isTrashed) {
        if (item.isTrashed) {
            value = runCatching {
                withContext(Dispatchers.IO) {
                    TrashRetentionReader.read(context.contentResolver, android.net.Uri.parse(item.uri), System.currentTimeMillis())
                }
            }.getOrNull()
        }
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        item {
            Text(stringResource(R.string.details), fontSize = 26.sp, modifier = Modifier.padding(bottom = 12.dp))
            DetailRow(stringResource(R.string.name), item.displayName)
            DetailRow(stringResource(R.string.type), item.mimeType)
            DetailRow(stringResource(R.string.size), formatBytes(item.sizeBytes))
            if (item.width > 0 && item.height > 0) DetailRow(stringResource(R.string.dimensions), "${item.width} x ${item.height}")
            item.durationMillis?.let { DetailRow(stringResource(R.string.duration), "${it / 1000} s") }
            item.bucketName?.let { DetailRow(stringResource(R.string.album), it) }
            DetailRow(stringResource(R.string.file_location), item.uri)
            retention?.let {
                DetailRow(
                    stringResource(R.string.trash_retention),
                    if (it.expired) stringResource(R.string.expired_mediastore) else stringResource(R.string.days_remaining, it.remainingDays),
                )
            }
        }
        details?.entries?.let { entries -> items(entries) { DetailRow(it.label, it.value) } }
        details?.let { value ->
            if (value.latitude != null && value.longitude != null) {
                item { DetailRow(stringResource(R.string.coordinates), "${value.latitude}, ${value.longitude}") }
            }
            if (value.histogram.any { it > 0 }) {
                item { Histogram(value.histogram) }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.36f))
        Text(value, modifier = Modifier.weight(0.64f))
    }
}

@Composable
private fun Histogram(values: IntArray) {
    val maximum = values.max().coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(stringResource(R.string.histogram), fontSize = 16.sp)
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp)) {
            val barWidth = size.width / values.size
            values.forEachIndexed { index, value ->
                val height = size.height * value.toFloat() / maximum
                drawLine(
                    color = Color.White,
                    start = Offset(index * barWidth, size.height),
                    end = Offset(index * barWidth, size.height - height),
                    strokeWidth = barWidth.coerceAtLeast(1f),
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format(Locale.ROOT, "%.2f GB", bytes / 1073741824.0)
    bytes >= 1024L * 1024L -> String.format(Locale.ROOT, "%.2f MB", bytes / 1048576.0)
    bytes >= 1024L -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
