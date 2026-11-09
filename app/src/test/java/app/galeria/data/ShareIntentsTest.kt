package app.galeria.data

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareIntentsTest {
    @Test
    fun sharesOneOrManyUrisWithReadAccess() {
        val one = MediaItem(1, "content://media/1", "uno.jpg", "image/jpeg", null, 1, 1, 1, 1, "Camera")
        val two = one.copy(id = 2, uri = "content://media/2", displayName = "dos.mp4", mimeType = "video/mp4")

        val single = ShareIntents.forItems(listOf(one))
        assertEquals(Intent.ACTION_SEND, single.action)
        assertEquals("image/jpeg", single.type)
        assertEquals(Uri.parse(one.uri), single.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertTrue(single.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

        val multiple = ShareIntents.forItems(listOf(one, two))
        assertEquals(Intent.ACTION_SEND_MULTIPLE, multiple.action)
        assertEquals("*/*", multiple.type)
        val uris = multiple.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        assertEquals(listOf(Uri.parse(one.uri), Uri.parse(two.uri)), uris)
        assertTrue(multiple.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
