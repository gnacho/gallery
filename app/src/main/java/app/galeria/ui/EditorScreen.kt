package app.galeria.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.galeria.data.BorderSpec
import app.galeria.data.CropSpec
import app.galeria.data.DrawingStroke
import app.galeria.data.ImageEditState
import app.galeria.data.ImageEditorRenderer
import app.galeria.data.ImageFilter
import app.galeria.data.MediaItem
import app.galeria.data.MosaicRegion
import app.galeria.data.NormalizedPoint
import app.galeria.data.TextOverlay
import app.galeria.data.WatermarkSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class EditorTool { NONE, DRAW, MOSAIC }

@Composable
internal fun EditorScreen(item: MediaItem, onClose: () -> Unit, onSaved: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val resolver = context.contentResolver
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(ImageEditState()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tool by remember { mutableStateOf(EditorTool.NONE) }
    var draft by remember { mutableStateOf<List<NormalizedPoint>>(emptyList()) }
    var magnifierPosition by remember { mutableStateOf<Offset?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var showTextDialog by remember { mutableStateOf(false) }
    var draftText by remember { mutableStateOf("") }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var draftWatermark by remember { mutableStateOf("") }
    var cropIndex by remember { mutableStateOf(0) }
    val source by produceState<Bitmap?>(initialValue = null, item.uri) {
        value = runCatching {
            withContext(Dispatchers.IO) { resolver.loadThumbnail(Uri.parse(item.uri), Size(2048, 2048), null) }
        }.getOrNull()
    }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(source, state) {
        source?.let { bitmap ->
            preview = withContext(Dispatchers.Default) { ImageEditorRenderer.render(bitmap, state) }
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, enabled = !saving) { Icon(GalleryIcons.Close, "Cerrar editor") }
            Text("Editar", fontSize = 22.sp, modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    state = state.reset()
                    cropIndex = 0
                    tool = EditorTool.NONE
                },
                enabled = !saving,
            ) { Text("Restablecer") }
            TextButton(
                enabled = source != null && !saving,
                onClick = {
                    saving = true
                    error = null
                    scope.launch(Dispatchers.IO) {
                        runCatching { ImageEditorRenderer.save(resolver, item, state) }
                            .onSuccess { withContext(Dispatchers.Main) { onSaved() } }
                            .onFailure {
                                withContext(Dispatchers.Main) {
                                    error = it.message
                                    saving = false
                                }
                            }
                    }
                },
            ) { Text("Guardar copia") }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                preview != null -> Image(
                    bitmap = requireNotNull(preview).asImageBitmap(),
                    contentDescription = "Vista previa editada",
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewSize = it }
                        .pointerInput(tool, preview) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (tool != EditorTool.NONE && viewSize.width > 0 && viewSize.height > 0) {
                                        magnifierPosition = offset
                                        val bitmap = requireNotNull(preview)
                                        val scale = minOf(
                                            viewSize.width / bitmap.width.toFloat(),
                                            viewSize.height / bitmap.height.toFloat(),
                                        )
                                        val contentWidth = bitmap.width * scale
                                        val contentHeight = bitmap.height * scale
                                        val point = NormalizedPoint(
                                            ((offset.x - (viewSize.width - contentWidth) / 2f) / contentWidth).coerceIn(0f, 1f),
                                            ((offset.y - (viewSize.height - contentHeight) / 2f) / contentHeight).coerceIn(0f, 1f),
                                        )
                                        draft = listOf(point)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (draft.isNotEmpty()) magnifierPosition = change.position
                                    if (draft.isNotEmpty()) {
                                        val bitmap = requireNotNull(preview)
                                        val scale = minOf(
                                            viewSize.width / bitmap.width.toFloat(),
                                            viewSize.height / bitmap.height.toFloat(),
                                        )
                                        val contentWidth = bitmap.width * scale
                                        val contentHeight = bitmap.height * scale
                                        val point = NormalizedPoint(
                                            ((change.position.x - (viewSize.width - contentWidth) / 2f) / contentWidth).coerceIn(0f, 1f),
                                            ((change.position.y - (viewSize.height - contentHeight) / 2f) / contentHeight).coerceIn(0f, 1f),
                                        )
                                        draft = if (tool == EditorTool.MOSAIC) listOf(draft.first(), point) else draft + point
                                    }
                                },
                                onDragEnd = {
                                    when (tool) {
                                        EditorTool.DRAW -> if (draft.isNotEmpty()) {
                                            state = state.copy(
                                                drawings = state.drawings + DrawingStroke(draft, android.graphics.Color.RED),
                                            )
                                        }
                                        EditorTool.MOSAIC -> if (draft.size >= 2) {
                                            val first = draft.first()
                                            val last = draft.last()
                                            val crop = CropSpec(
                                                minOf(first.x, last.x),
                                                minOf(first.y, last.y),
                                                maxOf(first.x, last.x),
                                                maxOf(first.y, last.y),
                                            )
                                            if (crop.right - crop.left > 0.01f && crop.bottom - crop.top > 0.01f) {
                                                state = state.copy(mosaics = state.mosaics + MosaicRegion(crop))
                                            }
                                        }
                                        EditorTool.NONE -> Unit
                                    }
                                    draft = emptyList()
                                    magnifierPosition = null
                                },
                                onDragCancel = {
                                    draft = emptyList()
                                    magnifierPosition = null
                                },
                            )
                        }
                        .then(
                            if (magnifierPosition == null) Modifier else Modifier.magnifier(
                                sourceCenter = { requireNotNull(magnifierPosition) },
                                magnifierCenter = {
                                    val position = requireNotNull(magnifierPosition)
                                    Offset(position.x, (position.y - 72.dp.toPx()).coerceAtLeast(0f))
                                },
                                zoom = 3f,
                            ),
                        ),
                    contentScale = ContentScale.Fit,
                )
                else -> CircularProgressIndicator()
            }
            if (tool != EditorTool.NONE) {
                Text(
                    if (tool == EditorTool.DRAW) "Arrastra para dibujar en rojo" else "Arrastra un rectángulo para pixelar",
                    modifier = Modifier.align(Alignment.BottomCenter).background(Color(0xAA000000)).padding(12.dp),
                )
            }
        }
        error?.let { Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(horizontal = 20.dp)) }
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Brillo", fontSize = 13.sp)
            Slider(
                value = state.brightness,
                onValueChange = { state = state.copy(brightness = it) },
                valueRange = -100f..100f,
            )
            Text("Contraste", fontSize = 13.sp)
            Slider(
                value = state.contrast,
                onValueChange = { state = state.copy(contrast = it) },
                valueRange = 0.5f..2f,
            )
            Text("Saturación", fontSize = 13.sp)
            Slider(
                value = state.saturation,
                onValueChange = { state = state.copy(saturation = it) },
                valueRange = 0f..2f,
            )
            Text("Calidez", fontSize = 13.sp)
            Slider(
                value = state.warmth,
                onValueChange = { state = state.copy(warmth = it) },
                valueRange = -100f..100f,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ImageFilter.entries) { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { state = state.copy(filter = filter) },
                        label = {
                            Text(
                                when (filter) {
                                    ImageFilter.ORIGINAL -> "Original"
                                    ImageFilter.MONO -> "Monocromo"
                                    ImageFilter.SEPIA -> "Sepia"
                                    ImageFilter.VIVID -> "Vívido"
                                },
                            )
                        },
                    )
                }
            }
            Text("Intensidad del filtro", fontSize = 13.sp)
            Slider(
                value = state.filterIntensity,
                onValueChange = { state = state.copy(filterIntensity = it) },
                valueRange = 0f..1f,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { state = state.rotateCounterClockwise() }) {
                    Icon(GalleryIcons.Rotate, null)
                    Text("Girar")
                }
                TextButton(onClick = { state = state.toggleFlip() }) {
                    Icon(GalleryIcons.Flip, null)
                    Text("Voltear")
                }
                TextButton(onClick = {
                    cropIndex = (cropIndex + 1) % 4
                    val bitmap = preview
                    state = if (cropIndex == 0 || bitmap == null) {
                        state.copy(crop = null)
                    } else {
                        val targetAspect = when (cropIndex) {
                            1 -> 1f
                            2 -> 4f / 3f
                            else -> 16f / 9f
                        }
                        val sourceAspect = bitmap.width / bitmap.height.toFloat()
                        val crop = if (targetAspect > sourceAspect) {
                            val fraction = sourceAspect / targetAspect
                            CropSpec(0f, (1f - fraction) / 2f, 1f, (1f + fraction) / 2f)
                        } else {
                            val fraction = targetAspect / sourceAspect
                            CropSpec((1f - fraction) / 2f, 0f, (1f + fraction) / 2f, 1f)
                        }
                        state.copy(crop = crop)
                    }
                }) {
                    Icon(GalleryIcons.Crop, null)
                    Text(if (cropIndex == 0) "Recortar" else "Recorte ${cropIndex}")
                }
                TextButton(onClick = { showTextDialog = true }) {
                    Icon(GalleryIcons.Text, null)
                    Text("Texto")
                }
                FilterChip(
                    selected = tool == EditorTool.DRAW,
                    onClick = { tool = if (tool == EditorTool.DRAW) EditorTool.NONE else EditorTool.DRAW },
                    label = { Text("Dibujar") },
                )
                FilterChip(
                    selected = tool == EditorTool.MOSAIC,
                    onClick = { tool = if (tool == EditorTool.MOSAIC) EditorTool.NONE else EditorTool.MOSAIC },
                    label = { Text("Mosaico") },
                )
                TextButton(
                    onClick = {
                        state = state.copy(
                            border = if (state.border == null) BorderSpec(0.03f, android.graphics.Color.WHITE) else null,
                        )
                    },
                ) { Text("Borde") }
                TextButton(
                    onClick = {
                        draftWatermark = state.watermark?.text.orEmpty()
                        showWatermarkDialog = true
                    },
                ) { Text("Marca") }
                TextButton(onClick = {
                    state = state.copy(watermark = WatermarkSpec("Noche lunar", lunar = true))
                }) { Text("Noche lunar") }
            }
        }
    }
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Añadir texto") },
            text = {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    label = { Text("Texto sobre la imagen") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draftText.isNotBlank(),
                    onClick = {
                        state = state.copy(
                            texts = state.texts + TextOverlay(draftText, 0.08f, 0.85f, android.graphics.Color.WHITE, 0.12f),
                        )
                        draftText = ""
                        showTextDialog = false
                    },
                ) { Text("Añadir") }
            },
            dismissButton = { TextButton(onClick = { showTextDialog = false }) { Text("Cancelar") } },
        )
    }
    if (showWatermarkDialog) {
        AlertDialog(
            onDismissRequest = { showWatermarkDialog = false },
            title = { Text("Marca de agua") },
            text = {
                OutlinedTextField(
                    value = draftWatermark,
                    onValueChange = { draftWatermark = it },
                    label = { Text("Texto de la marca") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draftWatermark.isNotBlank(),
                    onClick = {
                        state = state.copy(watermark = WatermarkSpec(draftWatermark.trim()))
                        showWatermarkDialog = false
                    },
                ) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { showWatermarkDialog = false }) { Text("Cancelar") } },
        )
    }
}
