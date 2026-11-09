package app.galeria.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.galeria.R
import app.galeria.data.AnnualReviewGenerator
import app.galeria.data.MediaItem
import java.time.ZoneId

@Composable
internal fun AnnualReviewScreen(items: List<MediaItem>, onOpen: (MediaItem) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val zone = remember { ZoneId.systemDefault() }
    val years = remember(items) { AnnualReviewGenerator.availableYears(items, zone) }
    var selectedYear by remember { mutableIntStateOf(years.firstOrNull() ?: 0) }
    val review = remember(items, selectedYear) { AnnualReviewGenerator.build(items, selectedYear, zone) }
    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(GalleryIcons.Back, stringResource(R.string.back_to_memories)) }
            Text(stringResource(R.string.annual_summary), modifier = Modifier.weight(1f))
        }
        if (years.isEmpty()) {
            MessageDestination(stringResource(R.string.no_dates_title), stringResource(R.string.no_dates_message))
            return
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(years, key = { it }) { year ->
                TextButton(onClick = { selectedYear = year }) {
                    Text(if (year == selectedYear) "[$year]" else "$year")
                }
            }
        }
        review?.let { current ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text(stringResource(R.string.images_count, current.imageCount))
                    Text(stringResource(R.string.videos_count, current.videoCount))
                    Text(stringResource(R.string.favorites_count, current.favoriteCount))
                    Text(stringResource(R.string.months_count, current.monthCount))
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                items(current.items, key = { it.uri }) { item ->
                    Box { MediaGridCell(item, onClick = { onOpen(item) }) }
                }
            }
        }
    }
}
