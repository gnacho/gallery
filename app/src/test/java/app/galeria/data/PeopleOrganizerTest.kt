package app.galeria.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PeopleOrganizerTest {
    @Test
    fun groupsNamedPeopleAndKeepsTheirMediaOrder() {
        val first = item(1)
        val second = item(2)
        val third = item(3)
        val assignments = mapOf("Nacho" to setOf(first.uri, third.uri), "Ana" to setOf(second.uri))

        val groups = PeopleOrganizer.groups(listOf(first, second, third), assignments)

        assertEquals(listOf("Ana", "Nacho"), groups.map { it.name })
        assertEquals(listOf(second), groups[0].items)
        assertEquals(listOf(first, third), groups[1].items)
    }

    @Test
    fun storeAddsAssignmentsWithoutRemovingOtherPeople() {
        val store = PeopleStore(RuntimeEnvironment.getApplication(), "people-test")
        val first = item(1)
        val second = item(2)

        store.assign("Nacho", listOf(first))
        store.assign("Nacho", listOf(second))
        store.assign("Ana", listOf(first))

        assertEquals(setOf(first.uri, second.uri), store.assignments()["Nacho"])
        assertEquals(setOf(first.uri), store.assignments()["Ana"])
    }

    private fun item(id: Long) = MediaItem(id, "content://media/$id", "$id.jpg", "image/jpeg", id)
}
