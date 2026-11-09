package app.galeria.ui

import android.app.Activity
import android.app.PendingIntent
import android.os.CancellationSignal
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.galeria.R
import app.galeria.data.AlbumTransferPlan
import app.galeria.data.GalleryContentFilter
import app.galeria.data.GallerySettings
import app.galeria.data.LibraryOrganizer
import app.galeria.data.LibraryOrganizerStore
import app.galeria.data.MediaItem
import app.galeria.data.MediaNotesStore
import app.galeria.data.MediaSelection
import app.galeria.data.MediaStoreOperations
import app.galeria.data.MediaStoreRequests
import app.galeria.data.PrivateVault
import app.galeria.data.VaultEntry
import app.galeria.data.SearchHistory
import app.galeria.data.ShareIntents
import app.galeria.data.VaultAuthenticator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GalleryApp(
    state: GalleryUiState,
    requestedEditor: MediaItem? = null,
    settings: GallerySettings,
    onSettings: (GallerySettings) -> Unit,
    favoriteItems: List<MediaItem>,
    trashItems: List<MediaItem>,
    isLoading: Boolean,
    loadError: String?,
    onDestination: (GalleryDestination) -> Unit,
    onOpen: (MediaItem) -> Unit,
    onCloseViewer: () -> Unit,
    onRetry: () -> Unit,
    onReload: () -> Unit,
) {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val organizerStore = remember { LibraryOrganizerStore(context) }
    val notesStore = remember { MediaNotesStore(context) }
    var notes by remember { mutableStateOf(notesStore.all()) }
    var organizer by remember { mutableStateOf(organizerStore.load()) }
    val rawItems = remember(state.sections) { state.sections.flatMap { it.items } }
    val views = remember(rawItems, organizer, settings.showVideos) {
        GalleryContentFilter.views(organizer.withCustomAlbums(organizer.visible(rawItems)), settings.showVideos)
    }
    val items = views.library
    val timelineItems = views.timeline
    var selection by remember { mutableStateOf(MediaSelection()) }
    var history by remember { mutableStateOf(SearchHistory()) }
    var editingItem by remember { mutableStateOf<MediaItem?>(null) }
    var videoEditingItem by remember { mutableStateOf<MediaItem?>(null) }
    LaunchedEffect(requestedEditor) {
        if (requestedEditor != null) editingItem = requestedEditor
    }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRename by remember { mutableStateOf<Pair<MediaItem, String>?>(null) }
    var albumDialogVisible by remember { mutableStateOf(false) }
    var albumName by remember { mutableStateOf("") }
    var renamingAlbum by remember { mutableStateOf<String?>(null) }
    var albumPendingDeletion by remember { mutableStateOf<String?>(null) }
    var transferDialogVisible by remember { mutableStateOf(false) }
    var transferIsMove by remember { mutableStateOf(false) }
    var transferAlbumName by remember { mutableStateOf("") }
    var pendingMove by remember { mutableStateOf<Pair<List<MediaItem>, AlbumTransferPlan>?>(null) }
    var settingsVisible by remember { mutableStateOf(false) }
    var vaultVisible by remember { mutableStateOf(false) }
    var mapVisible by remember { mutableStateOf(false) }
    var peopleVisible by remember { mutableStateOf(false) }
    var pendingVaultImports by remember { mutableStateOf<List<VaultEntry>>(emptyList()) }
    val vault = remember { lazy { PrivateVault(context) } }
    val scope = rememberCoroutineScope()
    val requestLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val vaultBatch = pendingVaultImports
        val moveBatch = pendingMove
        pendingVaultImports = emptyList()
        pendingMove = null
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingRename?.let { (item, newName) ->
                runCatching { MediaStoreOperations.rename(resolver, item, newName) }
                    .onFailure { message = "No se pudo renombrar" }
            }
            pendingRename = null
            if (moveBatch != null) {
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            moveBatch.first.sumOf { MediaStoreOperations.moveToAlbum(resolver, it, moveBatch.second) }
                        }
                    }.onSuccess { changed ->
                        selection = selection.clear()
                        message = "$changed elementos movidos a ${moveBatch.second.albumName.trim()}"
                        onReload()
                    }.onFailure { message = "No se pudieron mover los elementos" }
                }
            } else {
                selection = selection.clear()
                message = if (vaultBatch.isEmpty()) "Operación confirmada" else "Movido al cofre privado"
                if (vaultBatch.isNotEmpty()) onCloseViewer()
                onReload()
            }
        } else {
            pendingRename = null
            if (vaultBatch.isNotEmpty()) {
                scope.launch {
                    withContext(Dispatchers.IO) { vaultBatch.forEach { vault.value.rollback(it) } }
                    message = "Operación cancelada. No se movió ningún elemento"
                }
            } else {
                message = "Operación cancelada"
            }
        }
    }
    val launchRequest: (PendingIntent) -> Unit = { pendingIntent ->
        requestLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    }
    val share: (List<MediaItem>) -> Unit = { selected ->
        if (selected.isNotEmpty()) {
            context.startActivity(Intent.createChooser(ShareIntents.forItems(selected), "Compartir"))
        }
    }
    val trash: (List<MediaItem>) -> Unit = { selected ->
        if (selected.isNotEmpty()) {
            launchRequest(MediaStoreRequests.trash(resolver, selected, trashed = !selected.all { it.isTrashed }))
        }
    }
    val favorite: (List<MediaItem>) -> Unit = { selected ->
        if (selected.isNotEmpty()) {
            launchRequest(MediaStoreRequests.favorite(resolver, selected, favorite = !selected.all { it.isFavorite }))
        }
    }
    val delete: (List<MediaItem>) -> Unit = { selected ->
        if (selected.isNotEmpty()) launchRequest(MediaStoreRequests.delete(resolver, selected))
    }
    val duplicate: (MediaItem) -> Unit = { item ->
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { MediaStoreOperations.duplicate(resolver, item) } }
                .onSuccess {
                    message = "Copia creada"
                    onReload()
                }
                .onFailure { message = "No se pudo duplicar" }
        }
    }
    val rename: (MediaItem, String) -> Unit = { item, newName ->
        pendingRename = item to newName
        launchRequest(MediaStoreRequests.write(resolver, listOf(item)))
    }
    val transferToAlbum: (List<MediaItem>, AlbumTransferPlan, Boolean) -> Unit = { selected, plan, move ->
        if (selected.isNotEmpty()) {
            if (move) {
                pendingRename = null
                pendingMove = selected to plan
                runCatching { launchRequest(MediaStoreRequests.write(resolver, selected)) }
                    .onFailure {
                        pendingMove = null
                        message = "No se pudo solicitar acceso de escritura"
                    }
            } else {
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) { selected.forEach { MediaStoreOperations.copyToAlbum(resolver, it, plan) } }
                    }.onSuccess {
                        selection = selection.clear()
                        message = "Copiado a ${plan.albumName.trim()}"
                        onReload()
                    }.onFailure { message = "No se pudieron copiar los elementos" }
                }
            }
        }
    }
    val movePrivate: (List<MediaItem>) -> Unit = { selected ->
        if (selected.isNotEmpty()) {
            scope.launch {
                val imported = mutableListOf<VaultEntry>()
                val pendingIntent = runCatching {
                    selected.forEach { item ->
                        imported += withContext(Dispatchers.IO) { vault.value.importItem(item) }
                    }
                    MediaStoreRequests.delete(resolver, selected)
                }.getOrElse {
                    withContext(Dispatchers.IO) { imported.forEach { vault.value.rollback(it) } }
                    message = "No se pudo importar al cofre privado"
                    return@launch
                }
                pendingVaultImports = imported
                try {
                    launchRequest(pendingIntent)
                } catch (_: RuntimeException) {
                    pendingVaultImports = emptyList()
                    withContext(Dispatchers.IO) { imported.forEach { vault.value.rollback(it) } }
                    message = "No se pudo solicitar el borrado de los originales"
                }
            }
        }
    }

    val keyguard = remember { context.getSystemService(android.app.KeyguardManager::class.java) }
    var vaultAuthentication by remember { mutableStateOf<CancellationSignal?>(null) }
    DisposableEffect(Unit) {
        onDispose { vaultAuthentication?.cancel() }
    }

    val openPrivate: () -> Unit = {
        val activity = context as? Activity
        if (!keyguard.isDeviceSecure) {
            message = "Configura un bloqueo de pantalla para usar el cofre privado"
        } else if (activity == null) {
            message = "No se pudo abrir la autenticación del dispositivo"
        } else {
            vaultAuthentication?.cancel()
            vaultAuthentication = VaultAuthenticator(activity).authenticate(
                onSuccess = { vaultVisible = true },
                onError = { message = "No se desbloqueó el cofre privado: $it" },
            )
        }
    }

    val updateOrganizer: (LibraryOrganizer) -> Unit = {
        organizer = it
        organizerStore.save(it)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        val expanded = maxWidth >= 600.dp
        val selectedItems = items.filter { selection.contains(it.id) }
        when {
            vaultVisible -> PrivateVaultScreen(
                vault = vault.value,
                onChanged = onReload,
                onMessage = { message = it },
                onClose = { vaultVisible = false },
            )
            peopleVisible -> PeopleScreen(
                items = items,
                onClose = { peopleVisible = false },
                onOpen = {
                    peopleVisible = false
                    onOpen(it)
                },
            )
            mapVisible -> MapScreen(
                items = items,
                onClose = { mapVisible = false },
                onOpen = {
                    mapVisible = false
                    onOpen(it)
                },
            )
            settingsVisible -> SettingsScreen(
                settings = settings,
                hiddenAlbums = organizer.hiddenBuckets,
                onSettings = onSettings,
                onUnhideAlbum = { updateOrganizer(organizer.toggleHidden(it)) },
                onClose = { settingsVisible = false },
            )
            videoEditingItem != null -> VideoEditorScreen(
                item = requireNotNull(videoEditingItem),
                onClose = { videoEditingItem = null },
                onSaved = {
                    videoEditingItem = null
                    selection = selection.clear()
                    message = "Recorte guardado en Movies/Galeria"
                    onReload()
                },
            )
            editingItem != null -> EditorScreen(
                item = requireNotNull(editingItem),
                onClose = { editingItem = null },
                onSaved = {
                    editingItem = null
                    selection = selection.clear()
                    message = "Copia editada guardada"
                    onReload()
                },
            )
            state.viewerItem != null -> MediaViewer(
                items = when (val opened = requireNotNull(state.viewerItem)) {
                    in trashItems -> trashItems
                    in favoriteItems -> favoriteItems
                    else -> (if (state.destination == GalleryDestination.Gallery || state.destination == GalleryDestination.Photos) timelineItems else items)
                        .ifEmpty { listOf(opened) }
                },
                initialItem = requireNotNull(state.viewerItem),
                onClose = onCloseViewer,
                onShare = share,
                onFavorite = favorite,
                onPrivate = movePrivate,
                onTrash = trash,
                onDelete = delete,
                onEdit = { editingItem = it },
                onEditVideo = { videoEditingItem = it },
                onDuplicate = duplicate,
                onRename = rename,
                onMessage = { message = it },
                slideshowSeconds = settings.slideshowSeconds,
                notes = notes,
                onSaveNote = { item, note ->
                    notesStore.set(item.uri, note)
                    notes = notesStore.all()
                    message = if (note.isBlank()) "Nota eliminada" else "Nota guardada"
                },
            )
            else -> Row(modifier = Modifier.fillMaxSize()) {
                GalleryRail(
                    selected = state.destination,
                    expanded = expanded,
                    onDestination = onDestination,
                    onSettings = { settingsVisible = true },
                    modifier = Modifier.width(if (expanded) 160.dp else 80.dp),
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        state.permissionDenied -> PermissionState(onRetry)
                        isLoading -> LoadingState()
                        loadError != null -> ErrorState(loadError, onRetry)
                        else -> GalleryContent(
                            state = state,
                            items = items,
                            notes = notes,
                            timelineItems = timelineItems,
                            favoriteItems = favoriteItems,
                            trashItems = trashItems,
                            expanded = expanded,
                            selection = selection,
                            selectedItems = selectedItems,
                            history = history,
                            onHistory = { history = it },
                            onSelection = { selection = it },
                            onOpen = onOpen,
                            onRetry = onRetry,
                            onShare = share,
                            onTrash = trash,
                            onFavorite = favorite,
                            onDelete = delete,
                            onCreateAlbum = {
                                renamingAlbum = null
                                albumName = ""
                                albumDialogVisible = true
                            },
                            onMoveToAlbum = {
                                transferIsMove = true
                                transferAlbumName = ""
                                transferDialogVisible = true
                            },
                            onCopyToAlbum = {
                                transferIsMove = false
                                transferAlbumName = ""
                                transferDialogVisible = true
                            },
                            onEdit = { editingItem = it },
                            onEditVideo = { videoEditingItem = it },
                            onReload = onReload,
                            onMessage = { message = it },
                            slideshowSeconds = settings.slideshowSeconds,
                            onHideAlbum = { updateOrganizer(organizer.toggleHidden(it)) },
                            customAlbumNames = organizer.customAlbums.values.toSet(),
                            onRenameAlbum = {
                                renamingAlbum = it
                                albumName = it
                                albumDialogVisible = true
                            },
                            onDeleteAlbum = { albumPendingDeletion = it },
                            onPrivate = movePrivate,
                            onOpenPrivate = openPrivate,
                            onMap = { mapVisible = true },
                            onPeople = { peopleVisible = true },
                        )
                    }
                }
            }
        }
        if (transferDialogVisible) {
            AlertDialog(
                onDismissRequest = { transferDialogVisible = false },
                title = { Text(if (transferIsMove) "Mover a álbum" else "Copiar a álbum") },
                text = {
                    OutlinedTextField(
                        value = transferAlbumName,
                        onValueChange = { transferAlbumName = it },
                        label = { Text("Nombre del álbum de destino") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = transferAlbumName.isNotBlank(),
                        onClick = {
                            runCatching { AlbumTransferPlan(transferAlbumName) }
                                .onSuccess { plan ->
                                    transferToAlbum(selectedItems, plan, transferIsMove)
                                    transferDialogVisible = false
                                }
                                .onFailure { message = "El nombre del álbum no es válido" }
                        },
                    ) { Text(if (transferIsMove) "Mover" else "Copiar") }
                },
                dismissButton = { TextButton(onClick = { transferDialogVisible = false }) { Text("Cancelar") } },
            )
        }
        if (albumDialogVisible) {
            AlertDialog(
                onDismissRequest = { albumDialogVisible = false },
                title = { Text(if (renamingAlbum == null) "Nuevo álbum" else "Renombrar álbum") },
                text = {
                    OutlinedTextField(
                        value = albumName,
                        onValueChange = { albumName = it },
                        label = { Text("Nombre del álbum") },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = albumName.isNotBlank(),
                        onClick = {
                            val previousName = renamingAlbum
                            updateOrganizer(
                                if (previousName == null) organizer.assign(selectedItems, albumName)
                                else organizer.renameAlbum(previousName, albumName),
                            )
                            selection = selection.clear()
                            albumName = ""
                            renamingAlbum = null
                            albumDialogVisible = false
                            message = "Álbum actualizado"
                        },
                    ) { Text(if (renamingAlbum == null) "Crear" else "Guardar") }
                },
                dismissButton = { TextButton(onClick = { albumDialogVisible = false }) { Text("Cancelar") } },
            )
        }
        albumPendingDeletion?.let { album ->
            AlertDialog(
                onDismissRequest = { albumPendingDeletion = null },
                title = { Text("Eliminar álbum") },
                text = { Text("Se quitará la agrupación $album. No se borrará ninguna foto ni vídeo.") },
                confirmButton = {
                    TextButton(onClick = {
                        updateOrganizer(organizer.deleteAlbum(album))
                        albumPendingDeletion = null
                        message = "Álbum eliminado. Los elementos siguen en sus carpetas"
                    }) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { albumPendingDeletion = null }) { Text("Cancelar") } },
            )
        }
        message?.let {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                dismissAction = { TextButton(onClick = { message = null }) { Text("Cerrar") } },
            ) { Text(it) }
        }
    }
}

@Composable
private fun GalleryContent(
    state: GalleryUiState,
    items: List<MediaItem>,
    notes: Map<String, String>,
    timelineItems: List<MediaItem>,
    favoriteItems: List<MediaItem>,
    trashItems: List<MediaItem>,
    expanded: Boolean,
    selection: MediaSelection,
    selectedItems: List<MediaItem>,
    history: SearchHistory,
    onHistory: (SearchHistory) -> Unit,
    onSelection: (MediaSelection) -> Unit,
    onOpen: (MediaItem) -> Unit,
    onRetry: () -> Unit,
    onShare: (List<MediaItem>) -> Unit,
    onTrash: (List<MediaItem>) -> Unit,
    onFavorite: (List<MediaItem>) -> Unit,
    onDelete: (List<MediaItem>) -> Unit,
    onCreateAlbum: () -> Unit,
    onMoveToAlbum: () -> Unit,
    onCopyToAlbum: () -> Unit,
    onEdit: (MediaItem) -> Unit,
    onEditVideo: (MediaItem) -> Unit,
    onReload: () -> Unit,
    onMessage: (String) -> Unit,
    slideshowSeconds: Int,
    onHideAlbum: (Long) -> Unit,
    customAlbumNames: Set<String>,
    onRenameAlbum: (String) -> Unit,
    onDeleteAlbum: (String) -> Unit,
    onPrivate: (List<MediaItem>) -> Unit,
    onOpenPrivate: () -> Unit,
    onMap: () -> Unit,
    onPeople: () -> Unit,
) {
    when (state.destination) {
        GalleryDestination.Gallery, GalleryDestination.Photos -> TimelineScreen(
            title = if (state.destination == GalleryDestination.Gallery) "Galería" else "Fotos",
            items = timelineItems,
            notes = notes,
            expanded = expanded,
            selection = selection,
            history = history,
            onHistory = onHistory,
            onSelection = onSelection,
            onOpen = onOpen,
            onRetry = onRetry,
            onShare = onShare,
            onTrash = onTrash,
            onFavorite = onFavorite,
            onDelete = onDelete,
            onCreateAlbum = onCreateAlbum,
            onMoveToAlbum = onMoveToAlbum,
            onCopyToAlbum = onCopyToAlbum,
            onPrivate = onPrivate,
            onMap = onMap,
            onPeople = onPeople,
        )
        GalleryDestination.Albums -> AlbumsScreen(
            items = items,
            favoriteItems = favoriteItems,
            trashItems = trashItems,
            expanded = expanded,
            selection = selection,
            onSelection = onSelection,
            onOpen = onOpen,
            onRetry = onRetry,
            onShare = onShare,
            onTrash = onTrash,
            onFavorite = onFavorite,
            onDelete = onDelete,
            onCreateAlbum = onCreateAlbum,
            onMoveToAlbum = onMoveToAlbum,
            onCopyToAlbum = onCopyToAlbum,
            onHideAlbum = onHideAlbum,
            customAlbumNames = customAlbumNames,
            onRenameAlbum = onRenameAlbum,
            onDeleteAlbum = onDeleteAlbum,
            onPrivate = onPrivate,
            onOpenPrivate = onOpenPrivate,
            onMap = onMap,
            onPeople = onPeople,
        )
        GalleryDestination.Memories -> MemoriesScreen(items, onOpen, slideshowSeconds)
        GalleryDestination.Create -> CreateScreen(
            selectedItems = selectedItems,
            onEdit = onEdit,
            onEditVideo = onEditVideo,
            onReload = onReload,
            onMessage = onMessage,
        )
    }
}

@Composable
private fun GalleryRail(
    selected: GalleryDestination,
    expanded: Boolean,
    onDestination: (GalleryDestination) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GalleryDestination.entries.forEach { destination ->
            RailItem(destination, destination == selected, expanded) { onDestination(destination) }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onSettings)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(GalleryIcons.Settings, "Ajustes", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text("Ajustes", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RailItem(
    destination: GalleryDestination,
    selected: Boolean,
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (showLabel) Arrangement.Start else Arrangement.Center,
    ) {
        Icon(destination.icon(), destination.label, tint = color, modifier = Modifier.size(24.dp))
        if (showLabel) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                destination.label,
                color = color,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun DestinationTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 34.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
    )
}

@Composable
internal fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.loading), modifier = Modifier.padding(top = 18.dp))
    }
}

@Composable
internal fun PermissionState(onRetry: () -> Unit) = MessageDestination(
    stringResource(R.string.permission_title),
    stringResource(R.string.permission_message),
    stringResource(R.string.retry),
    onRetry,
)

@Composable
internal fun ErrorState(message: String, onRetry: () -> Unit) = MessageDestination(
    stringResource(R.string.app_name), message, stringResource(R.string.retry), onRetry,
)

@Composable
internal fun EmptyState(title: String, message: String, onRetry: (() -> Unit)?) =
    MessageDestination(title, message, onRetry?.let { stringResource(R.string.retry) }, onRetry)

@Composable
internal fun MessageDestination(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.padding(top = 18.dp)) {
                Text(actionLabel)
            }
        }
    }
}

private fun GalleryDestination.icon(): ImageVector = when (this) {
    GalleryDestination.Gallery -> GalleryIcons.Gallery
    GalleryDestination.Photos -> GalleryIcons.Photos
    GalleryDestination.Memories -> GalleryIcons.Memories
    GalleryDestination.Create -> GalleryIcons.Create
    GalleryDestination.Albums -> GalleryIcons.Albums
}
