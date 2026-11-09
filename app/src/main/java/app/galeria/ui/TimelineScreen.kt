package app.galeria.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.galeria.R
import app.galeria.data.MediaItem
import app.galeria.data.MediaSearch
import app.galeria.data.MediaSelection
import app.galeria.data.SearchHistory
import app.galeria.data.TimelineGrouper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TimelineScreen(
    title: String,
    items: List<MediaItem>,
    expanded: Boolean,
    selection: MediaSelection,
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
    onPrivate: (List<MediaItem>) -> Unit,
    onMap: () -> Unit,
    onPeople: () -> Unit,
    notes: Map<String, String> = emptyMap(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var columns by rememberSaveable { mutableIntStateOf(if (expanded) 5 else 4) }
    var moreVisible by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.systemDefault() }
    val filtered = remember(items, query, notes) { MediaSearch.filter(items, query, zone, notes) }
    val sections = remember(filtered) { TimelineGrouper.group(filtered, zone) }
    val selectedItems = items.filter { selection.contains(it.id) }
    val gridState = rememberLazyGridState()
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES")) }

    if (items.isEmpty()) {
        EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_message), onRetry)
        return
    }
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(columns) {
            detectTransformGestures { _, _, zoom, _ ->
                if (zoom > 1.08f) columns = (columns - 1).coerceAtLeast(2)
                if (zoom < 0.92f) columns = (columns + 1).coerceAtMost(8)
            }
        },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) { DestinationTitle(title) }
                    if (selection.count == 0) {
                        IconButton(onClick = { searchVisible = !searchVisible }) {
                            Icon(if (searchVisible) GalleryIcons.Close else GalleryIcons.Search, stringResource(R.string.search))
                        }
                        IconButton(onClick = onMap) { Icon(GalleryIcons.Map, stringResource(R.string.map)) }
                        IconButton(onClick = onPeople) { Icon(GalleryIcons.Person, stringResource(R.string.people)) }
                    }
                }
            }
            if (searchVisible && selection.count == 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            leadingIcon = { Icon(GalleryIcons.Search, null) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onHistory(history.record(query)) }),
                        )
                        if (query.isBlank() && history.entries.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                history.entries.forEach { entry ->
                                    TextButton(onClick = { query = entry }) { Text(entry) }
                                }
                            }
                        }
                    }
                }
            }
            if (selection.count > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onSelection(selection.clear()) }) {
                            Icon(GalleryIcons.Close, stringResource(R.string.cancel_selection))
                        }
                        Text(selection.count.toString(), modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { onSelection(selection.selectAll(filtered.map { it.id })) }) {
                            Icon(GalleryIcons.SelectAll, stringResource(R.string.select_all))
                        }
                        IconButton(onClick = { onShare(selectedItems) }) { Icon(GalleryIcons.Share, stringResource(R.string.share)) }
                        IconButton(onClick = { onFavorite(selectedItems) }) { Icon(GalleryIcons.Favorite, stringResource(R.string.favorites)) }
                        IconButton(onClick = { onTrash(selectedItems) }) { Icon(GalleryIcons.Delete, stringResource(R.string.move_to_trash)) }
                        Box {
                            IconButton(onClick = { moreVisible = true }) { Icon(GalleryIcons.More, stringResource(R.string.more)) }
                            DropdownMenu(expanded = moreVisible, onDismissRequest = { moreVisible = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.create_album)) },
                                    leadingIcon = { Icon(GalleryIcons.Album, null) },
                                    onClick = {
                                        moreVisible = false
                                        onCreateAlbum()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_to_album)) },
                                    leadingIcon = { Icon(GalleryIcons.Album, null) },
                                    onClick = {
                                        moreVisible = false
                                        onMoveToAlbum()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy_to_album)) },
                                    leadingIcon = { Icon(GalleryIcons.Copy, null) },
                                    onClick = {
                                        moreVisible = false
                                        onCopyToAlbum()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.move_to_private)) },
                                    leadingIcon = { Icon(GalleryIcons.Private, null) },
                                    onClick = {
                                        moreVisible = false
                                        onPrivate(selectedItems)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete_permanently)) },
                                    onClick = {
                                        moreVisible = false
                                        onDelete(selectedItems)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MessageDestination(stringResource(R.string.no_results_title), stringResource(R.string.no_results_message))
                }
            } else {
                sections.forEach { section ->
                    item(key = "date-${section.date}", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            section.date?.format(formatter) ?: stringResource(R.string.undated),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
                        )
                    }
                    items(section.items, key = { it.uri }) { item ->
                        MediaGridCell(
                            item = item,
                            selected = selection.contains(item.id),
                            onClick = {
                                if (selection.count > 0) onSelection(selection.toggle(item.id)) else onOpen(item)
                            },
                            onLongClick = { onSelection(selection.toggle(item.id)) },
                        )
                    }
                }
            }
        }
        if (gridState.isScrollInProgress) {
            val item = filtered.getOrNull(gridState.firstVisibleItemIndex)
            val date = item?.dateTakenMillis?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate().format(formatter)
            } ?: stringResource(R.string.undated)
            Text(
                date,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(24.dp)
                    .background(Color(0xCC17243A), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MediaGridCell(
    item: MediaItem,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        MediaImage(item = item, modifier = Modifier.fillMaxSize())
        if (selected) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)))
            Icon(
                GalleryIcons.SelectAll,
                stringResource(R.string.item_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp),
            )
        }
        if (item.mimeType.startsWith("video/")) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color(0x99000000), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(GalleryIcons.Video, stringResource(R.string.video_duration), tint = Color.White)
                item.durationMillis?.let {
                    Text(formatDuration(it), fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}
