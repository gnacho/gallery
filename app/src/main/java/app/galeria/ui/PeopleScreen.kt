package app.galeria.ui

import android.net.Uri
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.galeria.R
import app.galeria.data.FaceDetection
import app.galeria.data.MediaItem
import app.galeria.data.PeopleOrganizer
import app.galeria.data.PeopleStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PeopleScreen(
    items: List<MediaItem>,
    onClose: () -> Unit,
    onOpen: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { PeopleStore(context) }
    var detected by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var assignments by remember { mutableStateOf(store.assignments()) }
    var loading by remember { mutableStateOf(true) }
    var namingItem by remember { mutableStateOf<MediaItem?>(null) }
    var personName by remember { mutableStateOf("") }

    LaunchedEffect(items) {
        loading = true
        detected = withContext(Dispatchers.IO) {
            items.mapNotNull { item ->
                if (!item.mimeType.startsWith("image/")) return@mapNotNull null
                val bitmap = runCatching {
                    context.contentResolver.loadThumbnail(Uri.parse(item.uri), Size(512, 512), null)
                }.getOrNull() ?: return@mapNotNull null
                try {
                    if (FaceDetection.containsFace(bitmap)) item else null
                } finally {
                    bitmap.recycle()
                }
            }
        }
        loading = false
    }
    val groups = remember(detected, assignments) { PeopleOrganizer.groups(detected, assignments) }
    val namedUris = remember(assignments) { assignments.values.flatten().toSet() }
    val unnamed = detected.filter { it.uri !in namedUris }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(GalleryIcons.Back, stringResource(R.string.back)) }
            Text(stringResource(R.string.people), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.faces_count, detected.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            stringResource(R.string.people_help),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!loading && detected.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_faces), color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(groups, key = { it.name }) { group ->
                    Column {
                        Text(stringResource(R.string.faces_group, group.name, group.items.size), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(group.items, key = { it.uri }) { item ->
                                MediaImage(
                                    item = item,
                                    modifier = Modifier.padding(top = 8.dp).size(112.dp).clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpen(item) },
                                )
                            }
                        }
                    }
                }
                if (unnamed.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.unnamed_faces_count, unnamed.size), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                    }
                    items(unnamed.chunked(4)) { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            rowItems.forEach { item ->
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    MediaImage(
                                        item = item,
                                        modifier = Modifier.size(104.dp).clip(RoundedCornerShape(12.dp))
                                            .clickable { onOpen(item) },
                                    )
                                    TextButton(onClick = {
                                        personName = ""
                                        namingItem = item
                                    }) { Text(stringResource(R.string.name_action)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    namingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { namingItem = null },
            title = { Text(stringResource(R.string.name_person)) },
            text = {
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = personName.isNotBlank(),
                    onClick = {
                        store.assign(personName, listOf(item))
                        assignments = store.assignments()
                        namingItem = null
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { namingItem = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
