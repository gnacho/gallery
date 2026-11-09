package app.galeria.data

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GallerySettingsStoreTest {
    @Test
    fun restoresAppearanceAndPlaybackPreferences() {
        val context = RuntimeEnvironment.getApplication()
        val expected = GallerySettings(dynamicColor = false, showVideos = false, slideshowSeconds = 8)

        GallerySettingsStore(context).save(expected)

        assertEquals(expected, GallerySettingsStore(context).load())
    }
}
