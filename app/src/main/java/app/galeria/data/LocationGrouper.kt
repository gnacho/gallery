package app.galeria.data

import kotlin.math.roundToInt

data class MediaLocation(
    val item: MediaItem,
    val latitude: Double,
    val longitude: Double,
)

data class LocationCluster(
    val latitude: Double,
    val longitude: Double,
    val items: List<MediaItem>,
)

object LocationGrouper {
    fun group(locations: List<MediaLocation>): List<LocationCluster> {
        return locations
            .groupBy { location ->
                val latitude = (location.latitude * 1_000).roundToInt()
                val longitude = (location.longitude * 1_000).roundToInt()
                latitude to longitude
            }
            .values
            .map { cluster ->
                LocationCluster(
                    latitude = cluster.map { it.latitude }.average(),
                    longitude = cluster.map { it.longitude }.average(),
                    items = cluster.map { it.item },
                )
            }
            .sortedByDescending { it.items.size }
    }
}
