package app.galeria.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Slideshow(items = requireNotNull(playing).items, title = "Recuerdo de ${playing?.year}", slideshowSeconds = slideshowSeconds) {
            playing = null
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item { DestinationTitle("Recuerdos") }
        item {
            OutlinedButton(onClick = { annualVisible = true }, modifier = Modifier.padding(bottom = 16.dp)) {
                Text("Resumen anual")
            }
        }
        if (memories.isEmpty()) {
            item {
                MessageDestination(
                    "Aún no hay recuerdos",
                    "Aquí aparecerán fotos y vídeos de este mismo día en años anteriores.",
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
                                Text("Un día como hoy", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp)
                                Text("${memory.year}", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(onClick = { playing = memory }) {
                                Icon(GalleryIcons.Slideshow, "Reproducir recuerdo")
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
            IconButton(onClick = onClose) { Icon(GalleryIcons.Close, "Cerrar presentación") }
            Text(title, fontSize = 20.sp, modifier = Modifier.weight(1f))
            Text("${pager.currentPage + 1}/${items.size}", color = Color(0xFFC8CAD0))
        }
    }
}

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
        Slideshow(selectedItems, "Película instantánea") { slideshow = false }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { DestinationTitle("Crear") }
        item {
            Text(
                "Selecciona elementos en Fotos manteniendo pulsada una miniatura. Las creaciones se guardan como copias nuevas.",
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
                title = "Collage",
                description = "Combina de 2 a 9 imágenes en una composición cuadrada de 1600 x 1600.",
                action = if (creating) "Guardando..." else "Crear collage",
                enabled = images.size in 2..9 && !creating,
            ) {
                scope.launch {
                    creating = true
                    runCatching {
                        withContext(Dispatchers.IO) { CollageRenderer.create(context.contentResolver, images) }
                    }.onSuccess {
                        onMessage("Collage guardado en Pictures/Galeria")
                        onReload()
                    }.onFailure { onMessage("No se pudo crear el collage: ${it.message}") }
                    creating = false
                }
            }
        }
        item {
            CreationCard(
                title = "Edición de imagen",
                description = "Gira, voltea y ajusta filtro, brillo y contraste sin modificar el original.",
                action = "Editar imagen",
                enabled = images.size == 1,
            ) { onEdit(images.first()) }
        }
        item {
            CreationCard(
                title = "Imagen a vídeo",
                description = "Exporta las imágenes seleccionadas como MP4, cuatro segundos por imagen y sin alterar los originales.",
                action = if (creating) "Exportando..." else "Exportar MP4",
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
                                onMessage("Vídeo guardado en Movies/Galeria")
                                onReload()
                            }.onFailure { onMessage("No se pudo importar el vídeo: ${it.message}") }
                            file.delete()
                        }.onFailure {
                            onMessage("No se pudo exportar el vídeo: ${it.message}")
                        }
                        creating = false
                    }
                }
            }
        }
        item {
            CreationCard(
                title = "Presentación instantánea",
                description = "Reproduce una presentación de cuatro segundos por elemento.",
                action = "Reproducir",
                enabled = selectedItems.isNotEmpty(),
            ) { slideshow = true }
        }
        item {
            CreationCard(
                title = "Editor de vídeo",
                description = "Recorta el inicio y el fin y guarda una copia MP4 sin modificar el original.",
                action = "Recortar vídeo",
                enabled = selectedItems.count { it.mimeType.startsWith("video/") } == 1,
            ) {
                onEditVideo(selectedItems.first { it.mimeType.startsWith("video/") })
            }
        }
        item {
            Text(
                "La generación de vídeo con IA y la comunidad de fotos dependen de servicios privados de MagicOS y no se simulan como funciones locales.",
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
