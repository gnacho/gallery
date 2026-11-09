package app.galeria.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.galeria.R
import app.galeria.data.CollageRenderer
import app.galeria.data.MediaItem
import app.galeria.data.MemoryGrouper
import app.galeria.data.MemorySection
import app.galeria.data.MediaStoreOperations
import app.galeria.data.SlideshowPlan
import app.galeria.data.SlideshowVideoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.io.File
import java.util.UUID

@Composable
internal fun MemoriesScreen(items: List<MediaItem>, onOpen: (MediaItem) -> Unit, slideshowSeconds: Int = 4) {
    val memories = remember(items) { MemoryGrouper.group(items, LocalDate.now(), ZoneId.systemDefault()) }
    var playing by remember { mutableStateOf<MemorySection?>(null) }
    var annualVisible by remember { mutableStateOf(false) }
    if (annualVisible) {
        AnnualReviewScreen(items = items, onOpen = onOpen, onClose = { annualVisible = false })
        return
    }
    if (playing != null) {
        Slideshow(items = requireNotNull(playing).items, title = stringResource(R.string.memory_of_year, requireNotNull(playing).year), slideshowSeconds = slideshowSeconds) {
            playing = null
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item { DestinationTitle(stringResource(R.string.memories)) }
        item {
            OutlinedButton(onClick = { annualVisible = true }, modifier = Modifier.padding(bottom = 16.dp)) {
                Text(stringResource(R.string.annual_summary))
            }
        }
        if (memories.isEmpty()) {
            item {
                MessageDestination(
                    stringResource(R.string.no_memories_title),
                    stringResource(R.string.no_memories_message),
                )
            }
        } else {
            items(memories, key = { it.year }) { memory ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.on_this_day), color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp)
                                Text(memory.year.toString(), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(onClick = { playing = memory }) {
                                Icon(GalleryIcons.Slideshow, stringResource(R.string.play_memory))
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(memory.items, key = { it.uri }) { item ->
                                Box(modifier = Modifier.width(180.dp).height(180.dp)) {
                                    MediaGridCell(item, onClick = { onOpen(item) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun Slideshow(items: List<MediaItem>, title: String, slideshowSeconds: Int = 4, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val pager = rememberPagerState(pageCount = { items.size })
    LaunchedEffect(pager.currentPage, items) {
        if (items.isNotEmpty()) {
            delay(slideshowSeconds * 1000L)
            pager.animateScrollToPage((pager.currentPage + 1) % items.size)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            MediaImage(items[page], modifier = Modifier.fillMaxSize())
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0x88000000)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(GalleryIcons.Close, stringResource(R.string.close_slideshow)) }
            Text(title, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Text("${pager.currentPage + 1}/${items.size}", color = Color(0xFFC8CAD0))
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
internal fun CreateScreen(
    selectedItems: List<MediaItem>,
    onEdit: (MediaItem) -> Unit,
    onEditVideo: (MediaItem) -> Unit,
    onReload: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    val images = selectedItems.filter { it.mimeType.startsWith("image/") }
    if (slideshow && selectedItems.isNotEmpty()) {
        Slideshow(selectedItems, stringResource(R.string.instant_movie)) { slideshow = false }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DestinationTitle(stringResource(R.string.create)) }
        item {
            Text(
                stringResource(R.string.create_help),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (selectedItems.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(selectedItems, key = { it.uri }) { item ->
                        Box(modifier = Modifier.size(110.dp)) { MediaGridCell(item, onClick = {}) }
                    }
                }
            }
        }
        item {
            CreationCard(
                title = stringResource(R.string.collage),
                description = stringResource(R.string.collage_desc),
                action = if (creating) stringResource(R.string.saving) else stringResource(R.string.create_collage),
                enabled = images.size in 2..9 && !creating,
            ) {
                scope.launch {
                    creating = true
                    runCatching {
                        withContext(Dispatchers.IO) { CollageRenderer.create(context.contentResolver, images) }
                    }.onSuccess {
                        onMessage(context.getString(R.string.collage_saved))
                        onReload()
                    }.onFailure { onMessage(context.getString(R.string.collage_error, it.message)) }
                    creating = false
                }
            }
        }
        item {
            CreationCard(
                title = stringResource(R.string.image_edit),
                description = stringResource(R.string.image_edit_desc),
                action = stringResource(R.string.edit_image),
                enabled = images.size == 1,
            ) { onEdit(images.first()) }
        }
        item {
            CreationCard(
                title = stringResource(R.string.image_to_video),
                description = stringResource(R.string.image_to_video_desc),
                action = if (creating) stringResource(R.string.exporting) else stringResource(R.string.export_mp4),
                enabled = images.size in 1..30 && !creating,
            ) {
                creating = true
                val output = File(File(context.cacheDir, "exports"), "Pelicula_${UUID.randomUUID()}.mp4")
                SlideshowVideoExporter(context).export(SlideshowPlan(images), output) { result ->
                    scope.launch {
                        result.onSuccess { file ->
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    MediaStoreOperations.importFile(context.contentResolver, file, "Pelicula_${System.currentTimeMillis()}.mp4", "video/mp4")
                                }
                            }.onSuccess {
                                onMessage(context.getString(R.string.video_saved))
                                onReload()
                            }.onFailure { onMessage(context.getString(R.string.video_import_error, it.message)) }
                            file.delete()
                        }.onFailure {
                            onMessage(context.getString(R.string.video_export_error, it.message))
                        }
                        creating = false
                    }
                }
            }
        }
        item {
            CreationCard(
                title = stringResource(R.string.instant_slideshow),
                description = stringResource(R.string.instant_slideshow_desc),
                action = stringResource(R.string.play),
                enabled = selectedItems.isNotEmpty(),
            ) { slideshow = true }
        }
        item {
            CreationCard(
                title = stringResource(R.string.video_editor),
                description = stringResource(R.string.video_editor_desc),
                action = stringResource(R.string.trim_video),
                enabled = selectedItems.count { it.mimeType.startsWith("video/") } == 1,
            ) {
                onEditVideo(selectedItems.first { it.mimeType.startsWith("video/") })
            }
        }
        item {
            Text(
                stringResource(R.string.ai_notice),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun CreationCard(
    title: String,
    description: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            Button(onClick = onClick, enabled = enabled) { Text(action) }
        }
    }
}
