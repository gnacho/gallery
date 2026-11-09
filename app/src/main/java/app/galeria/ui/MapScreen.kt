package app.galeria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.galeria.data.LocationCluster
import app.galeria.data.LocationGrouper
import app.galeria.data.MediaItem
import app.galeria.data.MediaLocationReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
internal fun MapScreen(
    items: List<MediaItem>,
    onClose: () -> Unit,
    onOpen: (MediaItem) -> Unit,
) {
    val context = LocalContext.current
    var clusters by remember { mutableStateOf<List<LocationCluster>>(emptyList()) }
    var selected by remember { mutableStateOf<LocationCluster?>(null) }
    var loading by remember { mutableStateOf(true) }
    val map = remember {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", 0))
            userAgentValue = context.packageName
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(40.4168, -3.7038))
        }
    }

    LaunchedEffect(items) {
        loading = true
        clusters = withContext(Dispatchers.IO) {
            LocationGrouper.group(MediaLocationReader(context.contentResolver).read(items))
        }
        selected = clusters.firstOrNull()
        loading = false
    }
    LaunchedEffect(clusters) {
        map.overlays.clear()
        clusters.forEach { cluster ->
            val marker = Marker(map).apply {
                position = GeoPoint(cluster.latitude, cluster.longitude)
                title = "${cluster.items.size} elementos"
                snippet = cluster.items.take(4).joinToString { it.displayName }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { clicked, _ ->
                    selected = cluster
                    clicked.showInfoWindow()
                    true
                }
            }
            map.overlays.add(marker)
        }
        clusters.firstOrNull()?.let { map.controller.animateTo(GeoPoint(it.latitude, it.longitude)) }
        map.invalidate()
    }
    DisposableEffect(map) {
        map.onResume()
        onDispose {
            map.onPause()
            map.onDetach()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(GalleryIcons.Back, "Volver") }
            Text("Mapa", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("${clusters.sumOf { it.items.size }} ubicados", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(factory = { map }, modifier = Modifier.fillMaxSize())
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            if (!loading && clusters.isEmpty()) {
                Text(
                    "No hay fotografías con coordenadas EXIF",
                    modifier = Modifier.align(Alignment.Center).background(Color(0xCC000000), RoundedCornerShape(12.dp)).padding(16.dp),
                    color = Color.White,
                )
            }
        }
        selected?.let { cluster ->
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    "${cluster.items.size} elementos cerca de %.4f, %.4f".format(cluster.latitude, cluster.longitude),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(cluster.items, key = { it.id }) { item ->
                        MediaImage(
                            item = item,
                            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(10.dp)).clickable { onOpen(item) },
                        )
                    }
                }
            }
        }
    }
}
