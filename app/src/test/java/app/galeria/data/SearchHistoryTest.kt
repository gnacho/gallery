package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryTest {
    @Test
    fun keepsRecentUniqueNonBlankQueriesUpToEightEntries() {
        var history = SearchHistory()
        history = history.record("  Playa ")
        history = history.record("playa")
        assertEquals(listOf("playa"), history.entries)
        (1..9).forEach { history = history.record("consulta $it") }
        history = history.record("   ")

        assertEquals(8, history.entries.size)
        assertEquals("consulta 9", history.entries.first())
        assertEquals(0, history.entries.count { it.equals("playa", ignoreCase = true) })
        assertEquals(emptyList<String>(), SearchHistory().record(" ").entries)
    }
}
