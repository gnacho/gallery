package app.galeria.data

import android.content.Intent
import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PickerResultsTest {
    @Test
    fun returnsTheChosenUriWithReadAccess() {
        val uri = Uri.parse("content://media/7")

        val result = PickerResults.single(uri)

        assertEquals(uri, result.data)
        assertTrue(result.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun returnsEveryUriForAMultipleSelection() {
        val first = Uri.parse("content://media/7")
        val second = Uri.parse("content://media/8")

        val result = PickerResults.multiple(listOf(first, second))

        assertNull(result.data)
        assertEquals(listOf(first, second), (0 until result.clipData!!.itemCount).map { result.clipData!!.getItemAt(it).uri })
        assertTrue(result.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
