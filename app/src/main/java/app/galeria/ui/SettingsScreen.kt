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
import androidx.compose.ui.unit.dp
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
                Icon(GalleryIcons.Back, contentDescription = "Volver")
            }
            DestinationTitle("Ajustes")
        }
        SettingToggle(
            title = "Color dinámico",
            subtitle = "Usa los colores del sistema manteniendo el tema oscuro.",
            checked = settings.dynamicColor,
            onCheckedChange = { onSettings(settings.copy(dynamicColor = it)) },
        )
        SettingToggle(
            title = "Mostrar vídeos en Fotos",
            subtitle = "Los vídeos seguirán disponibles en sus álbumes aunque se oculten aquí.",
            checked = settings.showVideos,
            onCheckedChange = { onSettings(settings.copy(showVideos = it)) },
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Intervalo de presentación", style = MaterialTheme.typography.titleMedium)
        Text("${settings.slideshowSeconds} segundos por imagen", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = settings.slideshowSeconds.toFloat(),
            onValueChange = { onSettings(settings.copy(slideshowSeconds = it.toInt().coerceIn(2, 15))) },
            valueRange = 2f..15f,
            steps = 12,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Álbumes ocultos", style = MaterialTheme.typography.titleMedium)
        if (hiddenAlbums.isEmpty()) {
            Text("No hay álbumes ocultos.", modifier = Modifier.padding(vertical = 12.dp))
        } else {
            hiddenAlbums.sorted().forEach { albumId ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Álbum $albumId", modifier = Modifier.weight(1f))
                    IconButton(onClick = { onUnhideAlbum(albumId) }) {
                        Icon(GalleryIcons.Visibility, contentDescription = "Mostrar álbum $albumId")
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
