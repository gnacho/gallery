package app.galeria.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.galeria.data.AlbumGrouper
import app.galeria.data.MediaItem
import app.galeria.data.MediaSelection
import app.galeria.data.SearchHistory

private data class AlbumDetail(val name: String, val items: List<MediaItem>)

@Composable
internal fun AlbumsScreen(
    items: List<MediaItem>,
    favoriteItems: List<MediaItem>,
    trashItems: List<MediaItem>,
    expanded: Boolean,
    selection: MediaSelection,
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
    onHideAlbum: (Long) -> Unit,
    customAlbumNames: Set<String>,
    onRenameAlbum: (String) -> Unit,
    onDeleteAlbum: (String) -> Unit,
    onPrivate: (List<MediaItem>) -> Unit,
    onOpenPrivate: () -> Unit,
    onMap: () -> Unit,
    onPeople: () -> Unit,
) {
    val albums = remember(items) { AlbumGrouper.group(items) }
    var opened by remember { mutableStateOf<AlbumDetail?>(null) }
    if (opened != null) {
        val detail = requireNotNull(opened)
        BackHandler { opened = null }
        Column {
            if (detail.name == "Eliminados" && detail.items.isNotEmpty()) {
                TextButton(
                    onClick = { onDelete(detail.items) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Vaciar papelera") }
            }
            Box(modifier = Modifier.weight(1f)) {
                TimelineScreen(
                    title = detail.name,
                    items = detail.items,
                    expanded = expanded,
                    selection = selection,
                    history = SearchHistory(),
                    onHistory = {},
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
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (expanded) 4 else 2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { DestinationTitle("Álbumes") }
        item {
            SpecialAlbumCard(
                title = "Favoritos",
                items = favoriteItems,
                icon = GalleryIcons.Favorite,
                onClick = { opened = AlbumDetail("Favoritos", favoriteItems) },
            )
        }
        item {
            SpecialAlbumCard(
                title = "Privado",
                items = emptyList(),
                icon = GalleryIcons.Private,
                onClick = onOpenPrivate,
            )
        }
        item {
            SpecialAlbumCard(
                title = "Eliminados",
                items = trashItems,
                icon = GalleryIcons.Delete,
                onClick = { opened = AlbumDetail("Eliminados", trashItems) },
            )
        }
        if (albums.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MessageDestination("Sin álbumes", "Los álbumes se crean a partir de tus carpetas de imágenes y vídeos.")
            }
        } else {
            items(albums, key = { it.id }) { album ->
                Box {
                    MediaGridCell(album.cover, onClick = { opened = AlbumDetail(album.name, album.items) })
                    if (album.name in customAlbumNames) {
                        Row(modifier = Modifier.align(Alignment.TopStart).background(Color(0x88000000))) {
                            IconButton(onClick = { onRenameAlbum(album.name) }) {
                                Icon(GalleryIcons.Edit, contentDescription = "Renombrar ${album.name}")
                            }
                            IconButton(onClick = { onDeleteAlbum(album.name) }) {
                                Icon(GalleryIcons.Delete, contentDescription = "Eliminar álbum ${album.name}")
                            }
                        }
                    }
                    IconButton(
                        onClick = { onHideAlbum(album.id) },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(GalleryIcons.VisibilityOff, contentDescription = "Ocultar ${album.name}")
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color(0xAA000000))
                            .padding(10.dp),
                    ) {
                        Text(album.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("${album.count} elementos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecialAlbumCard(
    title: String,
    items: List<MediaItem>,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF181B20), RoundedCornerShape(6.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            items.firstOrNull()?.let { MediaImage(it, Modifier.fillMaxSize()) }
            if (items.isEmpty()) Icon(icon, null, modifier = Modifier.size(48.dp), tint = Color(0xFF8D9199))
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0xAA000000))
                    .padding(10.dp),
            ) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text("${items.size} elementos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
