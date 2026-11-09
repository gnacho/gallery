package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSelectionTest {
    @Test
    fun togglesItemsAndSelectsBatchesWithoutMutatingPreviousState() {
        val initial = MediaSelection()
        val one = initial.toggle(4)
        val two = one.toggle(9)

        assertEquals(emptySet<Long>(), initial.ids)
        assertEquals(setOf(4L, 9L), two.ids)
        assertTrue(two.contains(4))
        assertFalse(two.contains(8))
        assertEquals(setOf(4L), two.toggle(9).ids)
        assertEquals(setOf(4L, 9L, 12L), two.selectAll(listOf(12, 4)).ids)
        assertEquals(0, two.clear().count)
    }
}
