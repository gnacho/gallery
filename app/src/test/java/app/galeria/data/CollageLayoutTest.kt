package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CollageLayoutTest {
    @Test
    fun laysOutTwoFourAndNineImages() {
        val two = CollageLayout.forCount(2)
        assertEquals(2, two.columns)
        assertEquals(1, two.rows)
        assertEquals(CollageCell(0, 0, 0f, 0f, 0.5f, 1f), two.cells[0])

        val four = CollageLayout.forCount(4)
        assertEquals(2, four.columns)
        assertEquals(2, four.rows)
        assertEquals(CollageCell(3, 1, 0.5f, 0.5f, 0.5f, 0.5f), four.cells[3])

        val nine = CollageLayout.forCount(9)
        assertEquals(3, nine.columns)
        assertEquals(3, nine.rows)
        assertEquals(CollageCell(8, 2, 2f / 3f, 2f / 3f, 1f / 3f, 1f / 3f), nine.cells[8])
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMoreThanNineImages() {
        CollageLayout.forCount(10)
    }
}
