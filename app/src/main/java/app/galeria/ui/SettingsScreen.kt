package app.galeria.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.galeria.R
import app.galeria.data.GallerySettings

@Composable
internal fun SettingsScreen(
    settings: GallerySettings,
    hiddenAlbums: Set<Long>,
    onSettings: (GallerySettings) -> Unit,
    onUnhideAlbum: (Long) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(GalleryIcons.Back, contentDescription = stringResource(R.string.back))
            }
            DestinationTitle(stringResource(R.string.settings))
        }
        SettingToggle(
            title = stringResource(R.string.dynamic_color),
            subtitle = stringResource(R.string.dynamic_color_subtitle),
            checked = settings.dynamicColor,
            onCheckedChange = { onSettings(settings.copy(dynamicColor = it)) },
        )
        SettingToggle(
            title = stringResource(R.string.show_videos),
            subtitle = stringResource(R.string.show_videos_subtitle),
            checked = settings.showVideos,
            onCheckedChange = { onSettings(settings.copy(showVideos = it)) },
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.slideshow_interval), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.slideshow_seconds, settings.slideshowSeconds), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = settings.slideshowSeconds.toFloat(),
            onValueChange = { onSettings(settings.copy(slideshowSeconds = it.toInt().coerceIn(2, 15))) },
            valueRange = 2f..15f,
            steps = 12,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.hidden_albums), style = MaterialTheme.typography.titleMedium)
        if (hiddenAlbums.isEmpty()) {
            Text(stringResource(R.string.no_hidden_albums), modifier = Modifier.padding(vertical = 12.dp))
        } else {
            hiddenAlbums.sorted().forEach { albumId ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.hidden_album_item, albumId), modifier = Modifier.weight(1f))
                    IconButton(onClick = { onUnhideAlbum(albumId) }) {
                        Icon(GalleryIcons.Visibility, contentDescription = stringResource(R.string.unhide_album, albumId))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
