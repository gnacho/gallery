package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals

class RenamePlanTest {
    @Test
    fun preservesTheOriginalExtension() {
        assertEquals("after.jpg", RenamePlan.fileName("before.jpg", "after.png"))
        assertEquals("after.jpg", RenamePlan.fileName("before.jpg", "after"))
        assertEquals("after.jpg", RenamePlan.fileName("before.jpg", "after.jpg"))
    }
}
