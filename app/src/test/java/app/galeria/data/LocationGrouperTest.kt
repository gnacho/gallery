package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationGrouperTest {
    @Test
    fun groupsNearbyCoordinatesAndSortsByItemCount() {
        val first = item(1)
        val second = item(2)
        val third = item(3)
        val locations = listOf(
            MediaLocation(first, 40.41680, -3.70380),
            MediaLocation(third, 41.40000, 2.17000),
            MediaLocation(second, 40.41682, -3.70379),
        )

        val groups = LocationGrouper.group(locations)

        assertEquals(2, groups.size)
        assertEquals(listOf(first, second), groups[0].items)
        assertEquals(40.41681, groups[0].latitude, 0.000001)
        assertEquals(-3.703795, groups[0].longitude, 0.000001)
        assertEquals(listOf(third), groups[1].items)
    }

    private fun item(id: Long) = MediaItem(
        id = id,
        uri = "content://media/$id",
        displayName = "$id.jpg",
        mimeType = "image/jpeg",
        dateTakenMillis = id,
    )
}
