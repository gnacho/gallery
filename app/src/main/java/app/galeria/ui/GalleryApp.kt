package app.galeria.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.os.CancellationSignal
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.Crossfade
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

@SuppressLint("LocalContextGetResourceValueCall")
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
                    .onFailure { message = context.getString(R.string.rename_failed) }
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
                        message = context.getString(R.string.moved_to_album, changed, moveBatch.second.albumName.trim())
                        onReload()
                    }.onFailure { message = context.getString(R.string.move_failed) }
                }
            } else {
                selection = selection.clear()
                message = if (vaultBatch.isEmpty()) context.getString(R.string.operation_confirmed) else context.getString(R.string.moved_to_private)
                if (vaultBatch.isNotEmpty()) onCloseViewer()
                onReload()
            }
        } else {
            pendingRename = null
            if (vaultBatch.isNotEmpty()) {
                scope.launch {
                    withContext(Dispatchers.IO) { vaultBatch.forEach { vault.value.rollback(it) } }
                    message = context.getString(R.string.operation_cancelled_no_move)
                }
            } else {
                message = context.getString(R.string.operation_cancelled)
            }
        }
    }
    val launchRequest: (PendingIntent) -> Unit = { pendingIntent ->
        requestLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    }
    val share: (List<MediaItem>) -> Unit = { selected ->
        if (selected.isNotEmpty()) {
            context.startActivity(Intent.createChooser(ShareIntents.forItems(selected), context.getString(R.string.share)))
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
                    message = context.getString(R.string.copy_created)
                    onReload()
                }
                .onFailure { message = context.getString(R.string.duplicate_failed) }
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
                        message = context.getString(R.string.write_access_failed)
                    }
            } else {
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) { selected.forEach { MediaStoreOperations.copyToAlbum(resolver, it, plan) } }
                    }.onSuccess {
                        selection = selection.clear()
                        message = context.getString(R.string.copied_to_album, plan.albumName.trim())
                        onReload()
                    }.onFailure { message = context.getString(R.string.copy_failed) }
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
                    message = context.getString(R.string.import_vault_failed)
                    return@launch
                }
                pendingVaultImports = imported
                try {
                    launchRequest(pendingIntent)
                } catch (_: RuntimeException) {
                    pendingVaultImports = emptyList()
                    withContext(Dispatchers.IO) { imported.forEach { vault.value.rollback(it) } }
                    message = context.getString(R.string.delete_originals_failed)
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
            message = context.getString(R.string.vault_lock_required)
        } else if (activity == null) {
            message = context.getString(R.string.auth_open_failed)
        } else {
            vaultAuthentication?.cancel()
            vaultAuthentication = VaultAuthenticator(activity).authenticate(
                onSuccess = { vaultVisible = true },
                onError = { message = context.getString(R.string.vault_unlock_failed, it) },
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
                    message = context.getString(R.string.trim_saved)
                    onReload()
                },
            )
            editingItem != null -> EditorScreen(
                item = requireNotNull(editingItem),
                onClose = { editingItem = null },
                onSaved = {
                    editingItem = null
                    selection = selection.clear()
                    message = context.getString(R.string.edit_copy_saved)
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
                    message = if (note.isBlank()) context.getString(R.string.note_deleted) else context.getString(R.string.note_saved)
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
                    val contentState = when {
                        state.permissionDenied -> 0
                        isLoading -> 1
                        loadError != null -> 2
                        else -> 3
                    }
                    Crossfade(targetState = contentState, label = "content") { cs ->
                        when (cs) {
                            0 -> PermissionState(onRetry)
                            1 -> LoadingState()
                            2 -> ErrorState(loadError ?: "", onRetry)
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
        }
        if (transferDialogVisible) {
            AlertDialog(
                onDismissRequest = { transferDialogVisible = false },
                title = { Text(stringResource(if (transferIsMove) R.string.move_to_album else R.string.copy_to_album)) },
                text = {
                    OutlinedTextField(
                        value = transferAlbumName,
                        onValueChange = { transferAlbumName = it },
                        label = { Text(stringResource(R.string.destination_album_name)) },
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
                                .onFailure { message = context.getString(R.string.invalid_album_name) }
                        },
                    ) { Text(stringResource(if (transferIsMove) R.string.move else R.string.copy)) }
                },
                dismissButton = { TextButton(onClick = { transferDialogVisible = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
        if (albumDialogVisible) {
            AlertDialog(
                onDismissRequest = { albumDialogVisible = false },
                title = { Text(stringResource(if (renamingAlbum == null) R.string.new_album else R.string.rename_album_dialog)) },
                text = {
                    OutlinedTextField(
                        value = albumName,
                        onValueChange = { albumName = it },
                        label = { Text(stringResource(R.string.album_name_label)) },
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
                            message = context.getString(R.string.album_updated)
                        },
                    ) { Text(stringResource(if (renamingAlbum == null) R.string.create else R.string.save)) }
                },
                dismissButton = { TextButton(onClick = { albumDialogVisible = false }) { Text(stringResource(R.string.cancel)) } },
            )
        }
        albumPendingDeletion?.let { album ->
            AlertDialog(
                onDismissRequest = { albumPendingDeletion = null },
                title = { Text(stringResource(R.string.delete_album)) },
                text = { Text(stringResource(R.string.delete_album_confirm, album)) },
                confirmButton = {
                    TextButton(onClick = {
                        updateOrganizer(organizer.deleteAlbum(album))
                        albumPendingDeletion = null
                        message = context.getString(R.string.album_deleted)
                    }) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = { TextButton(onClick = { albumPendingDeletion = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }
        message?.let {
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                dismissAction = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.close)) } },
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
            title = if (state.destination == GalleryDestination.Gallery) stringResource(R.string.gallery) else stringResource(R.string.photos),
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
            Icon(GalleryIcons.Settings, stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expanded) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.settings), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Icon(destination.icon(), stringResource(destination.labelRes), tint = color, modifier = Modifier.size(24.dp))
        if (showLabel) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                stringResource(destination.labelRes),
                color = color,
                style = MaterialTheme.typography.labelLarge,
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
        style = MaterialTheme.typography.headlineLarge,
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
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
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
